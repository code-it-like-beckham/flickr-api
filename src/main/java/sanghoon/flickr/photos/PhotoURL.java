package sanghoon.flickr.photos;

import org.w3c.dom.Element;

public class PhotoURL {
	
	private String url;
	private int height;
	private int width;
	
	public PhotoURL(String url, int height, int width) {
		super();
		
		this.url = url;
		this.height = height;
		this.width = width;
	}
	
	public static PhotoURL extractPhotoURL(Element photoElement, String postfix) {

		String url_o = photoElement.getAttribute("url_" + postfix);
		if (url_o.isEmpty())
			return null;
		
		int height_o = Integer.parseInt(photoElement.getAttribute("height_" + postfix));
		int width_o = Integer.parseInt(photoElement.getAttribute("width_" + postfix));
		
		return new PhotoURL(url_o, height_o, width_o);
	}
}
