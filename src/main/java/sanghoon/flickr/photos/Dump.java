package sanghoon.flickr.photos;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import sanghoon.flickr.ApiKey;
import sanghoon.flickr.places.BoundingBox;

public class Dump {
	
	// flickr.photos.search API를 여러번 호출하여 주어진 위치의 (가능한) 모든 사진들을 다운로드
	
	
	private static Options createOptions() {

		Options options = new Options();

		// tags

		options.addOption("t", "tags", true, "A comma-delimited list of tags");

		// time

		options.addOption("m", "min-taken-date", true, "Minimum taken date (unix timestamp or mysql datetime).");
		options.addOption("n", "max-taken-date", true, "Maximum taken date (unix timestamp or mysql datetime).");

		// location

		options.addOption("b", "bounding-box", true,
				"A comma-delimited list of 4 values defining the Bounding Box of the area that will be searched (minimum_longitude,minimum_latitude,maximum_longitude,maximum_latitude).");

		// etc

//		options.addOption("f", "format", true, "An output format. json or xml (default)");

		options.addOption("o", "output-file-prefix", true, "A prefix of output files.");

		return options;
	}

	public static void main(String[] args) throws FileNotFoundException {
		
//		args = new String[] { "-b", "-122.373971375,47.55575575,-122.3455185,47.585350625000004" };

		if (args.length < 1) {

			String header = "\n"
					+ "Download all photos matching some criteria, using flickr.photos.search API."
					+ "\n\n";
			
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("java " + Dump.class.getName() + " [OPTIONS]...", header, createOptions(), "");
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

		String minTakenDate = line.hasOption("min-taken-date") ? line.getOptionValue("min-taken-date") : null;
		String maxTakenDate = line.hasOption("max-taken-date") ? line.getOptionValue("max-taken-date") : null;

		String bbox = line.hasOption("bounding-box") ? line.getOptionValue("bounding-box") : null;
		if (bbox == null) {
			System.err.println("bounding box parameter is required"); 
			return;
		}
		// TODO: places.find 와 places.getInfo 를 이용하여 id로부터 bounding box 알아내기
//		String woeID = line.hasOption("woe-id") ? line.getOptionValue("woe-id") : null;
//		String placeID = line.hasOption("place-id") ? line.getOptionValue("place-id") : null;

		String outputFilePrefix = line.hasOption("output-file-prefix") ? line.getOptionValue("output-file-prefix") : "output";
		
		ApiKey key = ApiKey.load(ClassLoader.getSystemResourceAsStream("flickr_api_key.properties"));

        Map<String, String> outputs = new HashMap<>();
        
        String[] coords = bbox.split(",");
        
        double minimum_longitude = Double.parseDouble(coords[0]);
        double minimum_latitude = Double.parseDouble(coords[1]);
        double maximum_longitude = Double.parseDouble(coords[2]);
        double maximum_latitude = Double.parseDouble(coords[3]);
        
        LinkedList<BoundingBox> bboxes = new LinkedList<>();
        bboxes.add(new BoundingBox(minimum_longitude, minimum_latitude, maximum_longitude, maximum_latitude));
        
        int index = 0;
        
        while (bboxes.isEmpty() == false) {
        	BoundingBox box = bboxes.pollFirst();
        	
        	int page = 1;
        	
        	System.out.print("searching for " + box.toString() + " .");
        	
        	Integer total = search(key, box, tags, minTakenDate, maxTakenDate, page, outputs);
        	
        	if (total == null) {
        		bboxes.addLast(box);
        		System.out.println(" failed, retry later");
        		continue;
        	} else if (MAX_PHOTOS_IN_A_BBOX < total) {
        		// Please note that Flickr will return at most the first 4,000 results for any given search query. 
        		// If this is an issue, we recommend trying a more specific query.

        		// 나눠서 다시
        		List<BoundingBox> splitBoxes = box.split();
        		for (BoundingBox splitBox : splitBoxes)
        			bboxes.addLast(splitBox);
        		
        		System.out.print(" " + total + " photos (>" + MAX_PHOTOS_IN_A_BBOX + "), split ");
        		
        		System.out.print("{");
        		for (int i = 0; i < splitBoxes.size(); i++) {
        			if (0 < i)
        				System.out.print(",");
        			System.out.print("[");
        			System.out.print(splitBoxes.get(i).toString());
        			System.out.print("]");
        		}
        		System.out.print("}");
        		System.out.println();
        		
        		continue;
        	} else if (PER_PAGE < total) {
        		// 한번의 search로 box 안의 조건에 맞는 사진을 모두 가져오지 못한다면
        		// (조건에 맞는 사진이 PER_PAGE보다 크다면), 
        		// page를 증가하며 여러번 호출
        		
        		while (page * PER_PAGE < total) {
        			page++;
        			search(key, box, tags, minTakenDate, maxTakenDate, page, outputs);
        			System.out.print(".");
        		}
        		System.out.println(" " + total + " photos in " + page + " pages");
        	}
        	
        	
        	if (MAX_PHOTOS_IN_AN_OUTPUT < outputs.size()) {
        		// 일정 수의 사진이 모아지면 디스크로 기록
        		
        		String filename = outputFilePrefix + "_" + index++ + ".json";
        		write(outputs, filename);
	            outputs.clear();
        	}
        }
        
        String filename = outputFilePrefix + "_" + index++ + ".json";
		write(outputs, filename);
		
        System.out.println("finished");
	}

	private static void write(Map<String, String> outputs, String filename) {
		PrintWriter writer;
		try {
			writer = new PrintWriter(filename);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
        for (String outputString : outputs.values())
        	writer.println(outputString);
        writer.close();
        
        System.out.println("wrote " + outputs.size() + " photos in " + filename);
        
        outputs.clear();
	}
	
	// 하나의 덤프 출력 파일에 포함되는 사진의 수
	// (사실 이보다 많을 수도 있고 적을 수도 있고...)
	private static final int MAX_PHOTOS_IN_AN_OUTPUT = 10000;

	// 하나의 쿼리 (bbox 혹은 tags)로 얻을 수 있는 최대 사진의 수
	// 실제 더 많은 사진들이 쿼리의 조건을 만족하더라도
	// 그리고 page 번호를 지정할 수 있다 하더라도
	// 접근 할 수 있는 최대 사진의 수는 제한되어 있다.
	// 이보다 많은 사진을 가져오려면 쿼리의 조건을 더 상세하게 지정해야 한다.
	private static final int MAX_PHOTOS_IN_A_BBOX = 4000;
	
	
	// 최대 4000개 까지 밖에 안됨
	// https://www.flickr.com/services/api/flickr.photos.search.html
	// bbox를 사용하여 쪼개가면서 질문 해야함
	
	// API call 사이의 지연 시간 (단위:millis)
	private static final long DELAY_TIME = 1100;
	
	// 한 페이지의 사진 수
	// 문서에는 500이 최대 지정 가능하다고 하지만, 실제 반환되는 사진의 수는 250이 최대
	private static final int PER_PAGE = 250;
	

	// output: <id , json>
	public static Integer search(ApiKey key, BoundingBox box,
			String tags, String minTakenDate, String maxTakenDate, 
			int page, Map<String, String> output) {
		
		assert key != null;
		assert 0 < page;
		assert output != null;
		
		
		// API call 사이의 시간 지연
		// (혹시나 해서...)
		try {
			Thread.sleep(DELAY_TIME);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

        OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.flickr.com/services/rest/");

        request.setConnectTimeout(30, TimeUnit.SECONDS);
        request.setReadTimeout(30, TimeUnit.SECONDS);
        
        request.addQuerystringParameter("api_key", key.getKey());
        
        request.addQuerystringParameter("method", "flickr.photos.search");
        request.addQuerystringParameter("extras", "description,license,date_upload,date_taken,owner_name,icon_server,original_format,last_update,geo,tags,machine_tags,o_dims,views,media,path_alias,url_sq,url_t,url_s,url_q,url_m,url_n,url_z,url_c,url_l,url_o");

		
        request.addQuerystringParameter("bbox", box.toString());

        if (tags != null)
        	request.addQuerystringParameter("tags", tags);
        
        if (minTakenDate != null)
        	request.addQuerystringParameter("min_taken_date", minTakenDate);
        
        if (maxTakenDate != null)
        	request.addQuerystringParameter("max_taken_date", maxTakenDate);
        
    	request.addQuerystringParameter("per_page", Integer.toString(PER_PAGE));
    	request.addQuerystringParameter("page", Integer.toString(page));
        
        request.addQuerystringParameter("format", "json");

		// send request and receive response

        Response scribeResponse = request.send();

        String jsonString = null;
        try {
        	jsonString = scribeResponse.getBody().trim();
        } catch (Exception e) {
        	System.err.println(e.getMessage());
        	return null;
        }

        // header 제거
        jsonString = jsonString.substring(14, jsonString.length() - 1);

        // parsing json
        
        JsonParser parser = new JsonParser();
        
        JsonObject jsonObject = parser.parse(jsonString).getAsJsonObject();
        String stat = jsonObject.get("stat").getAsString();
        if ("ok".equals(stat) == false) {
        	System.err.println("실패 " + page);
        	System.err.println(jsonObject);
        	return null;
        }
        
        JsonObject jsonPhotos = jsonObject.get("photos").getAsJsonObject();
        
        int retpage = jsonPhotos.get("page").getAsInt();
        int pages = jsonPhotos.get("pages").getAsInt();
        int retperPage = jsonPhotos.get("perpage").getAsInt();
        int total = jsonPhotos.get("total").getAsInt();
        
        JsonArray photo = jsonPhotos.getAsJsonArray("photo");
        
        for (int i = 0; i < photo.size(); i++) {
        	JsonObject jsonPhoto = photo.get(i).getAsJsonObject();
        	String id = jsonPhoto.get("id").getAsString();
        	
        	if (output.containsKey(id))
        		continue;
        	output.put(id, jsonPhoto.toString());
        }
        
        return total;
	}

}
