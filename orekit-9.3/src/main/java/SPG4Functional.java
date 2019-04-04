
import org.orekit.attitudes.Attitude;

import org.orekit.attitudes.AttitudeProvider;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.frames.TransformProvider;
import org.orekit.models.earth.EarthShape;
import org.orekit.models.earth.Geoid;
import org.orekit.propagation.analytical.tle.*;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import com.opencsv.CSVWriter;




public class SPG4Functional {
	private final static double a = 6378137; // radius
	private final static double e = 8.1819190842622e-2;  // eccentricity

	private final static double asq = Math.pow(a,2);
	private final static double esq = Math.pow(e,2);
	
	public static void main(String[] args)
	{
		
		File orekitData = new File("C:\\Users\\Rushi\\eclipse-workspace\\orekit-9.3-sources");
		DataProvidersManager manager = DataProvidersManager.getInstance();
		manager.addProvider(new DirectoryCrawler(orekitData));
		String csv = "/Users/Rushi/csv/SpacetrackResults.csv"; //csv file for input
		String date_time_range = "2008-09-15T15:53:00Z|2008-09-15T17:53:00Z";  //date and time for input
		String duration = "00:20:00";    //duration between data capture
		Functional(csv,date_time_range,duration);
	}
	
	public static double[] eceftolla(double x,double y,double z)
	{
		double b = Math.sqrt( asq * (1-esq) );
		double bsq = Math.pow(b,2);
		double ep = Math.sqrt( (asq - bsq)/bsq);
		double p = Math.sqrt( Math.pow(x,2) + Math.pow(y,2) );
		double th = Math.atan2(a*z, b*p);

		double lon = Math.atan2(y,x);
		double lat = Math.atan2( (z + Math.pow(ep,2)*b*Math.pow(Math.sin(th),3) ), (p - esq*a*Math.pow(Math.cos(th),3)) );
		double N = a/( Math.sqrt(1-esq*Math.pow(Math.sin(lat),2)) );
		double alt = p / Math.cos(lat) - N;
		lon = lon % (2*Math.PI);
		double[] lla = {lat,lon,alt};
		return lla;
	}
	
	public static void Functional(String csv, String date_time_range,String duration)
	{
		BufferedReader br = null;
		String line = "";
		String split = ",";
		TLE[] Satellites = new TLE[4000];     //Holds satellites TLE data
		int counter = 0;
		TimeComponents step = TimeComponents.parseTime(duration);
		double totalSeconds = step.getHour()*3600 + step.getMinute()*60 + step.getSecond();
		int position = date_time_range.indexOf('|');
		String start = date_time_range.substring(0,position);    //Extracts the start time given
		String end = date_time_range.substring(position+1);      //Extracts the end time given
		DateTimeComponents startTemp = DateTimeComponents.parseDateTime(start);
		DateTimeComponents endTemp = DateTimeComponents.parseDateTime(end);
		TimeScale utc = TimeScalesFactory.getUTC();
		AbsoluteDate initialDate = new AbsoluteDate(startTemp,utc);
		AbsoluteDate endDate = new AbsoluteDate(endTemp,utc);
		Frame inertialFrame = FramesFactory.getEME2000();
		final PVCoordinatesProvider earth = CelestialBodyFactory.getEarth();
		String[][] sat_info = new String[4000][50];
		
		try
		{
			br = new BufferedReader(new FileReader(csv));
			br.readLine();
			while((line = br.readLine()) != null)
			{
				String[] sat = line.split(split);
				sat_info[counter] = sat;
				TLE temp = new TLE(sat[24],sat[25]);
				Satellites[counter] = temp;
				counter++;
			}
			AbsoluteDate extrapDate = initialDate;
			PVCoordinatesProvider pvCoordinates = null;
			AttitudeProvider attitude = null;
			File file = new File("outputfile.csv");
			FileWriter outputfile = new FileWriter(file);
			CSVWriter writer  = new CSVWriter(outputfile);
			String[] header = {"norad_cat_id","object_name","object_type","classification_type","object_id","object_number","timestamp","latitude","longitude","altitude"};
			writer.writeNext(header);
			double x;
			double y;
			double z;
			double lla[];
			while(extrapDate.compareTo(endDate) <=0)
			{
				for(int i = 0; i < counter; i++)
				{
					
				PVCoordinates finalLoc = TLEPropagator.selectExtrapolator(Satellites[i]).getPVCoordinates(extrapDate);
				System.out.print(extrapDate + " ");
				
				String norad_id = Integer.toString(Satellites[i].getSatelliteNumber());
				String object_name = sat_info[i][4];
				String object_type = sat_info[i][5];
				String class_type = sat_info[i][6];
				String object_id = sat_info[i][26];
				String object_number = sat_info[i][27];
				String time_stamp = extrapDate.toString();
				String location = finalLoc.toString();
				x = finalLoc.getPosition().getX();
				y = finalLoc.getPosition().getY();
				z = finalLoc.getPosition().getZ();
				lla = eceftolla(x,y,z);
				System.out.println(lla[0]  + " " + lla[1] + " " + lla[2]);		
				String latString = Double.toString(lla[0]);
				String lonString = Double.toString(lla[1]);
				String altString = Double.toString(lla[2]);
				String[] Line = {norad_id,object_name,object_type,class_type,object_id,object_number,time_stamp,latString,lonString,altString};
				writer.writeNext(Line);
				}
				extrapDate = extrapDate.shiftedBy(totalSeconds);
			}
			writer.close();
			
			
		}
		catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
	}

	

}
