package sanghoon.flickr.photos;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Photo {

	private String id;
	
	private String owner;
	private String ownerName;
	
	private String secret;
	private String server;
	private String title;
	
	private boolean isPublic;
	private boolean isFriend;
	private boolean isFamily;
	
	private String license;
	
	private Date dateUpload = null;
	private Date lastUpload = null;
	
	private String dateTaken;
	private String dateTakenGranularity;
	private String dateTakenUnknown;

	private String icon_server;
	private String icon_farm;
	
	private String views;
	private String[] tags;
	private String machine_tags;

	private String original_secret;
	private String original_format;

	private double latitude;
	private double longitude;

	private int accuracy;

	private String context;

	private String place_id;
	private String woeid;

	private String geo_is_family;
	private String geo_is_friend;
	private String geo_is_contact;
	private String geo_is_public;

	private String media;
	private String media_status;

	private PhotoURL photo_sq;
	private PhotoURL photo_t;
	private PhotoURL photo_s;
	private PhotoURL photo_q;
	private PhotoURL photo_m;
	private PhotoURL photo_n;
	private PhotoURL photo_z;
	private PhotoURL photo_c;
	private PhotoURL photo_l;
	private PhotoURL photo_o;
	
	private String path_alias;
	private String description;
	
	public static List<Photo> parseList(String xmlString) {
		
//		JsonParser parser = new JsonParser();
//      JsonObject jsonObject = parser.parse(jsonString).getAsJsonObject();
//      return parse(jsonObject);
		
		InputStream stream = new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8));

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = null;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			return Collections.emptyList();
		}
		Document doc;
		try {
			doc = dBuilder.parse(stream);
		} catch (SAXException | IOException e) {
			e.printStackTrace();
			return Collections.emptyList();
		}
		
		Element photosElement = doc.getDocumentElement();

		NodeList photoList = photosElement.getElementsByTagName("photo");
		
		List<Photo> output = new ArrayList<>();
		
		for (int i = 0; i < photoList.getLength(); i++) {
			Node photoNode = photoList.item(i);
			if (photoNode instanceof Element) {
				Photo photo = parse((Element) photoNode);
				output.add(photo);
			}
		}
		
		return output;
	}
	
//	public static Photo parse(String xmlString) throws SAXException, IOException, ParserConfigurationException {
//		
//		InputStream stream = new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8));
//
//		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
//		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
//		Document doc = dBuilder.parse(stream);
//		
//		Element photoElement = doc.getDocumentElement();
//		
//		return parse(photoElement);
//	}
	
	public static Photo parse(Element photoElement) {
		Photo photo = new Photo();
		
		photo.id = photoElement.getAttribute("id"); // basic
		photo.owner = photoElement.getAttribute("owner"); // basic
		photo.secret = photoElement.getAttribute("secret"); // basic
		photo.server = photoElement.getAttribute("server"); // basic
		photo.title = photoElement.getAttribute("title"); // basic
		
		photo.isPublic = "1".equals(photoElement.getAttribute("ispublic")); // basic
		photo.isFriend = "1".equals(photoElement.getAttribute("isfriend")); // basic
		photo.isFamily = "1".equals(photoElement.getAttribute("isfamily")); // basic
		
		photo.license = photoElement.getAttribute("license");
		
		// Posted dates
// The 'posted' date represents the time at which the photo was uploaded to Flickr. This is set at the time of upload.
// The posted date is always passed around as a unix timestamp, which is an unsigned integer specifying the number of seconds since Jan 1st 1970 GMT.
// All posted dates are passed around in GMT and it's up to the application provider to format them using the relevant viewer's timezone.

		// https://www.flickr.com/services/api/misc.dates.html
		if (photoElement.hasAttribute("dateupload")) {
			String dateupload = photoElement.getAttribute("dateupload");
			long date_upload = Long.parseLong(dateupload); // 1456362755
			photo.dateUpload = new Date(date_upload * 1000L);
		}
		
		if (photoElement.hasAttribute("lastupdate")) {
			String lastupdate = photoElement.getAttribute("lastupdate");
			long last_update = Long.parseLong(lastupdate); // 1456362757
			photo.lastUpload = new Date(last_update * 1000L);
		}		
		
		photo.dateTaken = photoElement.getAttribute("datetaken");
		photo.dateTakenGranularity = photoElement.getAttribute("datetakengranularity");
		photo.dateTakenUnknown = photoElement.getAttribute("datetakenunknown");
		
		
		photo.ownerName = photoElement.getAttribute("ownername");
		
		photo.icon_server = photoElement.getAttribute("iconserver");
		photo.icon_farm = photoElement.getAttribute("iconfarm");
		
		photo.views = photoElement.getAttribute("views");
		
		
		photo.tags = photoElement.getAttribute("tags").split("\\s");
		
		photo.machine_tags = photoElement.getAttribute("machine_tags"); // ???
		
		photo.original_secret = photoElement.getAttribute("original_secret");
		photo.original_format = photoElement.getAttribute("original_format");
		
		if (photoElement.hasAttribute("latitude"))
			photo.latitude = Double.parseDouble(photoElement.getAttribute("latitude"));
		
		if (photoElement.hasAttribute("longitude"))
			photo.longitude = Double.parseDouble(photoElement.getAttribute("longitude"));
		
		if (photoElement.hasAttribute("accuracy"))
		photo.accuracy = Integer.parseInt(photoElement.getAttribute("accuracy"));
		
		photo.context = photoElement.getAttribute("context");
		
		
		// -------------
		
		
		photo.place_id = photoElement.getAttribute("place_id");
		
		photo.woeid = photoElement.getAttribute("woeid");
		
		photo.geo_is_family = photoElement.getAttribute("geo_is_family");
		photo.geo_is_friend = photoElement.getAttribute("geo_is_friend");
		photo.geo_is_contact = photoElement.getAttribute("geo_is_contact");
		photo.geo_is_public = photoElement.getAttribute("geo_is_public");
		
		photo.media = photoElement.getAttribute("media"); // e.g., photo
		photo.media_status = photoElement.getAttribute("media_status"); // e.g., ready
		

		// A comma-delimited list of extra information to fetch for each returned record. 
		// Currently supported fields are: 
		// description, license, date_upload, date_taken, owner_name, icon_server, original_format, 
		// last_update, geo, tags, machine_tags, 
		// o_dims, views, media, path_alias, 
		// url_sq, url_t, url_s, url_q, url_m, url_n, url_z, url_c, url_l, url_o

		photo.photo_sq = PhotoURL.extractPhotoURL(photoElement, "sq");
		photo.photo_t = PhotoURL.extractPhotoURL(photoElement, "t");
		photo.photo_s = PhotoURL.extractPhotoURL(photoElement, "s");
		photo.photo_q = PhotoURL.extractPhotoURL(photoElement, "q");
		photo.photo_m = PhotoURL.extractPhotoURL(photoElement, "m");
		photo.photo_n = PhotoURL.extractPhotoURL(photoElement, "n");
		photo.photo_z = PhotoURL.extractPhotoURL(photoElement, "z");
		photo.photo_c = PhotoURL.extractPhotoURL(photoElement, "c");
		photo.photo_l = PhotoURL.extractPhotoURL(photoElement, "l");
		photo.photo_o = PhotoURL.extractPhotoURL(photoElement, "o");
		
		photo.path_alias = photoElement.getAttribute("pathalias");

		NodeList descriptionNL = photoElement.getElementsByTagName("description");
		if (0 < descriptionNL.getLength())
			photo.description  = descriptionNL.item(0).getTextContent();
		
		
		// geo에 대한 실제 예는 어떤 것일까?
//		String geo = photoElement.getAttribute("geo");
		
		// o_dims 는?
//		String o_dims = photoElement.getAttribute("o_dims");
		
		return photo;
	}
	

	
}
