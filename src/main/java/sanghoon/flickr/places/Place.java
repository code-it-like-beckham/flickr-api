package sanghoon.flickr.places;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Place {

	private String text = null;

	private String place_id = null;
	private String woeid = null;

	private String place_url = null;
	private String place_type = null;
	private String place_type_id = null;
	private String timezone = null;
	private String woe_name = null;
	
	// < [lat,lon], ... >
	private List<double[]> polylines = null;
	
	private static final int LATITUDE_IN_POLY = 0;
	private static final int LONGITUDE_IN_POLY = 1;
	
	private double min(int a) {
		double min = Double.MAX_VALUE;
		for (double[] polyline : polylines) {
			if (polyline[a] < min)
				min = polyline[a];
		}
		return min;
	}

	public double minLatitude() {
		return min(LATITUDE_IN_POLY);
	}
	
	public double minLongitude() {
		return min(LONGITUDE_IN_POLY);
	}

	private double max(int a) {
		double max = Double.MIN_VALUE;
		for (double[] polyline : polylines) {
			if (max < polyline[a])
				max = polyline[a];
		}
		return max;
	}
	
	public double maxLatitude() {
		return max(LATITUDE_IN_POLY);
	}
	
	public double maxLongitude() {
		return max(LONGITUDE_IN_POLY);
	}

	@Override
	public String toString() {
		return "Places [" + text + "]";
	}
	
	public static List<Place> parseList(String xmlString) {
		InputStream stream = new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8));

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		Document doc = null;
		try {
			DocumentBuilder dBuilder;
			dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(stream);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
			return Collections.emptyList();
		}

		Element firstElement = doc.getDocumentElement();
		Element places = (Element) firstElement.getFirstChild().getNextSibling();

		String q = places.getAttribute("query");
		int total = Integer.parseInt(places.getAttribute("total"));
		
		List<Place> output = new ArrayList<>();

		NodeList children = places.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Element childElement = (Element) child;
				Place place = parse(childElement);
				output.add(place);
			}
		}
		
		return output;
	}
	
	public static Place parse(String xmlString) {
		InputStream stream = new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8));

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		Document doc = null;
		try {
			DocumentBuilder dBuilder;
			dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(stream);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
			return null;
		}

		return parse(doc.getDocumentElement());
	}
	
	public static Place parse(Element childElement) {

		Place place = new Place();
		
		place.text = childElement.getFirstChild().getTextContent();

		place.place_id = childElement.getAttribute("place_id");
		place.woeid = childElement.getAttribute("woeid");

		place.place_url = childElement.getAttribute("place_url");
		place.place_type = childElement.getAttribute("place_type");
		place.place_type_id = childElement.getAttribute("place_type_id");
		place.timezone = childElement.getAttribute("timezone");
		place.woe_name = childElement.getAttribute("woe_name");
		
		boolean has_shapedata = "1".equals(childElement.getAttribute("has_shapedata"));
		
		if (has_shapedata) {
			// TODO: 제대로 되는지 확인하기
			
			place.polylines = new ArrayList<>();
			
			NodeList polyline = childElement.getElementsByTagName("polyline");
			
			String polylineText = polyline.item(0).getTextContent();
			String[] latlons = polylineText.split(" ");
			for (String latlon : latlons) {
				String[] a = latlon.split(",");
				double lat = Double.parseDouble(a[0]);
				double lon = Double.parseDouble(a[1]);
				place.polylines.add(new double[] { lat, lon });
			}
		}

//		<locality place_id="4hLQygSaBJ92" woeid="3534" latitude="45.512" longitude="-73.554" place_url="/Canada/Quebec/Montreal">Montreal</locality>
//		  <county place_id="cFBi9x6bCJ8D5rba1g" woeid="29375198" latitude="45.551" longitude="-73.600" place_url="/cFBi9x6bCJ8D5rba1g">Montreal</county>
//		  <region place_id="CrZUvXebApjI0.72" woeid="2344924" latitude="53.890" longitude="-68.429" place_url="/Canada/Quebec">Quebec</region>
//		  <country place_id="EESRy8qbApgaeIkbsA" woeid="23424775" latitude="62.358" longitude="-96.582" place_url="/Canada">Canada</country>
//		  <shapedata created="1223513357" alpha="0.012359619140625" count_points="34778" count_edges="52" has_donuthole="1" is_donuthole="1">
//		    <polylines>
//		      <polyline>
//		            45.427627563477,-73.589645385742 45.428966522217,-73.587898254395, etc...
//		         </polyline>
//		    </polylines>
//		    <urls>
//		      <shapefile>
//		         http://farm4.static.flickr.com/3228/shapefiles/3534_20081111_0a8afe03c5.tar.gz
//		         </shapefile>
//		    </urls>
//		  </shapedata>
		
		return place;
	}

	
}
