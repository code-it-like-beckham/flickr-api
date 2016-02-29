package sanghoon.flickr.places;

import java.util.ArrayList;
import java.util.List;

public class BoundingBox {

	private double minimumLongitude;// = -122.459330;
	private double minimumLatitude;// = 47.496566;
	private double maximumLongitude;// = -122.231707;
	private double maximumLatitude;// = 47.733325;

	public BoundingBox(double minimumLongitude, double minimumLatitude,
			double maximumLongitude, double maximumLatitude) {
		super();
		
		this.minimumLongitude = minimumLongitude;
		this.minimumLatitude = minimumLatitude;
		this.maximumLongitude = maximumLongitude;
		this.maximumLatitude = maximumLatitude;
	}

    public double minimumLongitude() {
    	return minimumLongitude;
    }
    
    public double minimumLatitude() {
    	return minimumLatitude;
    }
    
    public double maximumLongitude() {
    	return maximumLongitude;
    }
    
    public double maximumLatitude() {
    	return maximumLatitude;
    }
	
	public List<BoundingBox> split() {
		List<BoundingBox> output = new ArrayList<>();
		
		double midX = (minimumLongitude + maximumLongitude) / 2.0;
		double midY = (minimumLatitude + maximumLatitude) / 2.0;
		
		output.add(new BoundingBox(minimumLongitude, minimumLatitude, midX, midY));
		output.add(new BoundingBox(minimumLongitude, midY, midX, maximumLatitude));
		output.add(new BoundingBox(midX, midY, maximumLongitude, maximumLatitude));
		output.add(new BoundingBox(midX, minimumLatitude, maximumLongitude, midY));
		
		return output;
	}

	@Override
	public String toString() {
		return minimumLongitude + "," + minimumLatitude + "," + maximumLongitude + "," + maximumLatitude;
	}

}
