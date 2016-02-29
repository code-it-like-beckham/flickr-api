package sanghoon.flickr.photos;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;

import sanghoon.flickr.ApiKey;
import sanghoon.flickr.OutputFormat;

public class Search {

	public static void main(String[] args) {

		if (args.length < 1) {
			
			String header = "\n"
					+ "Return a list of photos matching some criteria. "
					+ "\n"
					+ "https://www.flickr.com/services/api/flickr.photos.search.html"
					+ "\n\n";
			
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("java " + Search.class.getName() + " [OPTIONS]...", header, createOptions(), "");
			return;
		}
		
		CommandLineParser parser = new DefaultParser();
		CommandLine line;
		try {
			line = parser.parse(createOptions(), args);
		} catch (ParseException exp) {
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
			return;
		}

		String tags = line.hasOption("tags") ? line.getOptionValue("tags") : null;
		String machineTags = line.hasOption("machine-tags") ? line.getOptionValue("machine-tags") : null;

		String minUploadDate = line.hasOption("min-upload-date") ? line.getOptionValue("min-upload-date") : null;
		String maxUploadDate = line.hasOption("max-upload-date") ? line.getOptionValue("max-upload-date") : null;

		String minTakenDate = line.hasOption("min-taken-date") ? line.getOptionValue("min-taken-date") : null;
		String maxTakenDate = line.hasOption("max-taken-date") ? line.getOptionValue("max-taken-date") : null;

		String bbox = line.hasOption("bounding-box") ? line.getOptionValue("bounding-box") : null;
		String woeID = line.hasOption("woe-id") ? line.getOptionValue("woe-id") : null;
		String placeID = line.hasOption("place-id") ? line.getOptionValue("place-id") : null;

		int page = line.hasOption("page") ? Integer.parseInt(line.getOptionValue("page")) : 1;
		OutputFormat outputFormat = line.hasOption("format")
				? OutputFormat.valueOf(line.getOptionValue("format").toUpperCase()) : OutputFormat.XML;

		File outputFile = line.hasOption("output-file") ? new File(line.getOptionValue("output-file")) : null;


		ApiKey key = ApiKey.load(ClassLoader.getSystemResourceAsStream("flickr_api_key.properties"));
		
		long beginTime = System.currentTimeMillis();

		List<Photo> photoList = run(key, tags, machineTags, minUploadDate, maxUploadDate, minTakenDate, maxTakenDate,
				bbox, woeID, placeID, page, outputFormat, outputFile);

		System.err.println(
				photoList.size() + " photos are retrieved (" + (System.currentTimeMillis() - beginTime) + "ms).");
	}

	private static Options createOptions() {

		Options options = new Options();

		// tags

		options.addOption("t", "tags", true, "A comma-delimited list of tags");
		options.addOption("m", "machine-tags", true, "");

		// time

		options.addOption("mud", "min-upload-date", true, "Minimum upload date (unix timestamp or mysql datetime).");
		options.addOption("nud", "max-upload-date", true, "Maximum upload date (unix timestamp or mysql datetime).");

		options.addOption("mtd", "min-taken-date", true, "Minimum taken date (unix timestamp or mysql datetime).");
		options.addOption("ntd", "max-taken-date", true, "Maximum taken date (unix timestamp or mysql datetime).");

		// location

		// options.addOption("g", "has-geo", false, "Any photo that has been
		// geotagged.");

		options.addOption("b", "bounding-box", true,
				"A comma-delimited list of 4 values defining the Bounding Box of the area that will be searched (minimum_longitude,minimum_latitude,maximum_longitude,maximum_latitude).");

		options.addOption("wid", "woe-id", true,
				"A 32-bit identifier that uniquely represents spatial entities. (not used if bbox argument is present).");
		options.addOption("pid", "place-id", true, "A Flickr place id. (not used if bbox argument is present).");

		// etc

		options.addOption("p", "page", true, "The page of results to return (default 1).");

		options.addOption("f", "format", true, "An output format: XML (default) or JSON.");

		options.addOption("o", "output-file", true, "An output filename.");

		return options;
	}

	public static List<Photo> run(ApiKey key, String tags, String machineTags, String minUploadDate,
			String maxUploadDate, String minTakenDate, String maxTakenDate,
			String bbox, String woeID, String placeID, int page, OutputFormat outputFormat, File outputFile) {

		assert key != null;

		OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.flickr.com/services/rest/");

		request.setConnectTimeout(30, TimeUnit.SECONDS);
		request.setReadTimeout(30, TimeUnit.SECONDS);

		request.addQuerystringParameter("method", "flickr.photos.search");
		request.addQuerystringParameter("extras", 
				"description,license,date_upload,date_taken,owner_name,icon_server,original_format,last_update,geo,tags,machine_tags,o_dims,views,media,path_alias,url_sq,url_t,url_s,url_q,url_m,url_n,url_z,url_c,url_l,url_o");

		request.addQuerystringParameter("api_key", key.getKey());

		// query options

		// tags

		if (tags != null)
			request.addQuerystringParameter("tags", tags);

		if (machineTags != null)
			request.addQuerystringParameter("machine_tags", machineTags);

		// time

		if (minUploadDate != null)
			request.addQuerystringParameter("min_upload_date", minUploadDate);

		if (maxUploadDate != null)
			request.addQuerystringParameter("max_upload_date", maxUploadDate);

		if (minTakenDate != null)
			request.addQuerystringParameter("min_taken_date", minTakenDate);

		if (maxTakenDate != null)
			request.addQuerystringParameter("max_taken_date", maxTakenDate);

		// location

		if (bbox != null) {
			request.addQuerystringParameter("bbox", bbox);
		} else {
			if (woeID != null)
				request.addQuerystringParameter("woe_id", woeID);
			else if (placeID != null)
				request.addQuerystringParameter("place_id", placeID);
		}

		// etc

		request.addQuerystringParameter("paqe", Integer.toString(page));

		request.addQuerystringParameter("format", outputFormat.name().toLowerCase());
		
		// send request and receive response

		Response scribeResponse = request.send();

		String outputString = null;
		try {
			outputString = scribeResponse.getBody().trim();
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

		// output
		if (outputFile != null) {
			// file output
			PrintWriter writer = null;
			try {
				writer = new PrintWriter(new FileOutputStream(outputFile, true));
				writer.println(outputString);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} finally {
				if (writer != null)
					writer.close();
			}
		} else {
			// console output if output file is not specified
			System.out.println(outputString);
		}

		return Photo.parseList(outputString);
	}

}