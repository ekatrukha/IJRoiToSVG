package ijroitosvg;

import java.awt.Color;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.OvalRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.io.SaveDialog;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;

public class IJRoiToSVG implements PlugIn 
{
	
	boolean bUseImage = false;
	
	float fScaleFactor = 1.0f;

	String sUnits = "mm";
	
	boolean bRescale = true;
	
	int rescaledX = 287;
	
	int rescaledY = 200;

	@Override
	public void run( String arg )
	{

		RoiManager rm = RoiManager.getInstance();
		if (rm == null) 
		{
			IJ.showMessage( "ROIs in ROI manager." );
			return;
		}
		
		int nWOut = 1;
		int nHOut = 1;
		
		if(bUseImage)
		{
			final ImagePlus imp = IJ.getImage();
	
			if (imp == null)
			{
				IJ.noImage();
				return;
			}
			nWOut = imp.getWidth();
			nHOut = imp.getHeight();
		}
		String filename = getTimestamp() + "_IJroisToSVG";
		String lastDir = Prefs.get( "IJRoiToSVG.lastDir", "" );
		SaveDialog sd = new SaveDialog("Save ROIs as SVG", lastDir, filename, ".svg");
		String path = sd.getDirectory();
		if (path == null)
			return;
		lastDir = path;
		Prefs.set( "IJRoiToSVG.lastDir", lastDir );
		filename = path + sd.getFileName();
		final Roi[] rois = rm.getRoisAsArray();
		if(!bUseImage)
		{
			final Rectangle bounds = estimateAllROIsBounds(rois);
			nWOut = bounds.width;
			nHOut = bounds.height;		
		}
		if(bRescale)
		{
			float fFactor = Math.min(rescaledX/(float)nWOut, rescaledY/(float)nHOut);
			fScaleFactor = fFactor;
			nWOut = rescaledX;
			nHOut = rescaledY;
		}
		String out = exportSvg( rois, nWOut, nHOut);

		try {
			final File file = new File(filename);

			try (FileWriter writer = new FileWriter(file))
			{
				writer.write( out );
				writer.close();
			}
		} catch (IOException e) {	
			IJ.log(e.getMessage());

		}
	}
	
	String exportSvg(final Roi[] rois, final int width, final int height) 
	{
	    StringBuilder sb = new StringBuilder();
	    sb.append( "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" );
	    sb.append(String.format(
	        "<svg \n    xmlns=\"http://www.w3.org/2000/svg\" \n    width=\"%d" + sUnits
	        +"\" \n    height=\"%d" + sUnits + "\"" +
	        "\n    viewBox=\"0 0 %d %d\""
	        +">\n",
	        width, height, width, height
	    ));

	    for (Roi roi : rois) {
	        sb.append("  ").append(roiToSvg(roi))
	        .append( "\n    id=\"" + roi.getName() + "\"" )
	        .append( "/>\n");
	    }

	    sb.append("</svg>");
	    return sb.toString();
	}
	
	String roiToSvg(final Roi roi) 
	{
		final int nType = roi.getType();
		switch(nType )
		{
		case Roi.RECTANGLE:
			return rectToSvg(roi); 
		case Roi.POLYGON:
			return polygonToSvg((PolygonRoi) roi);
		case Roi.OVAL:
			return ovalToSvg((OvalRoi) roi);
		default:
			return pathToSvg(roi);
		}
	}
	
	String rectToSvg(final Roi roi) 
	{
	    Rectangle b = roi.getBounds();
	    return String.format(
	        "<rect \n    x=\"%.2f\" \n    y=\"%.2f\" \n    width=\"%.2f\" \n    height=\"%.2f\" "
	        + getROIColorFillStroke (roi),
	        b.x * fScaleFactor, 
	        b.y * fScaleFactor, 
	        b.width  * fScaleFactor, 
	        b.height * fScaleFactor
	    );
	}
	
	String polygonToSvg(final PolygonRoi roi) 
	{
	    Polygon p = roi.getPolygon();

	    StringBuilder points = new StringBuilder();
	    for (int i = 0; i < p.npoints; i++) {
	        points.append(String.format("%.2f,",p.xpoints[i] * fScaleFactor))
	        	  .append(String.format("%.2f ",p.ypoints[i] * fScaleFactor));
	    }

	    return String.format(
	        "<polygon \n    points=\"%s\""
       		+ getROIColorFillStroke (roi),
	        points.toString().trim()
	    );
	}
	
	String ovalToSvg(final OvalRoi roi) 
	{
	    Rectangle b = roi.getBounds();

	    double cx = ( b.x + b.width / 2.0 )  * fScaleFactor;
	    double cy = (b.y + b.height / 2.0 ) *  fScaleFactor;
	    double rx = b.width / 2.0   * fScaleFactor;
	    double ry = b.height / 2.0 *  fScaleFactor;

	    return String.format(
	        "<ellipse \n    cx=\"%.2f\" \n    cy=\"%.2f\" \n    rx=\"%.2f\" \n    ry=\"%.2f\" " 
	        + getROIColorFillStroke (roi),
	        cx, cy, rx, ry
	    );
	}
	
	String pathToSvg(final Roi roi) 
	{
	    FloatPolygon fp = roi.getFloatPolygon();

	    StringBuilder d = new StringBuilder();

	    for (int i = 0; i < fp.npoints; i++) 
	    {
	        if (i == 0) 
	        {
	            d.append(String.format("M %.2f %.2f ", fp.xpoints[i] * fScaleFactor, fp.ypoints[i] * fScaleFactor));
	        } 
	        else 
	        {
	            d.append(String.format("L %.2f %.2f ", fp.xpoints[i] * fScaleFactor, fp.ypoints[i] * fScaleFactor));
	        }
	    }

	    if (roi.isArea()) {
	        d.append("Z");
	    }

	    return String.format(
	        "<path \n    d=\"%s\"" 
	        + getROIColorFillStroke (roi),
 	        d.toString()
	    );
	}
	
    public static String getTimestamp() 
    {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
        return LocalDateTime.now().format(formatter);
    }
    
    String getROIColorFillStroke(final Roi roi)
    {
    	return getROIFillString( roi ) + getROIStrokeColorString( roi ) + getROIStrokeWidthString( roi );
    }
    
    String getROIStrokeColorString(final Roi roi)
    {
		Color color = roi.getStrokeColor();
		if(color == null)
		{
			color = Roi.getColor();
		}
		return  "\n    stroke=\"" + String.format("#%02x%02x%02x", 
                color.getRed(), 
                color.getGreen(), 
                color.getBlue()) + "\"";
    }
    
    String getROIStrokeWidthString(final Roi roi)
    {
    	float fStrokeWidth = Math.max( roi.getStrokeWidth(), 1.0f);
    	return " \n    stroke-width=\"" + String.format( "%.2f", fStrokeWidth * fScaleFactor ) + "\"";
    }

    String getROIFillString(final Roi roi)
    {
    	Color color = roi.getFillColor();
    	if( color == null)
    	{
    		return "\n    fill=\"none\" ";
    	}
    	
    	return "\n    fill=\"" + String.format("#%02x%02x%02x", 
                color.getRed(), 
                color.getGreen(), 
                color.getBlue()) + "\"";
    }

    
    Rectangle estimateAllROIsBounds(final Roi[] rois)
    {
    	int nW = 0;
    	int nH = 0;
    	for (Roi roi : rois) 
    	{
    		final Rectangle rect = roi.getBounds();
    		nW = Math.max( nW, rect.x + rect.width );
    		nH = Math.max( nH, rect.y + rect.height );
    	}
    	return new Rectangle(0, 0, nW, nH);
    }

	/**
	 * Main method for debugging.
	 *
	 * For debugging, it is convenient to have a method that starts ImageJ, loads
	 * an image and calls the plugin, e.g. after setting breakpoints.
	 *
	 * @param args unused
	 */
	public static void main(String[] args) throws Exception 
	{
		new ImageJ();
		ImagePlus image = IJ.openImage("/home/eugene/Desktop/projects/IJROIsToSVG/test.tif");
		//ImagePlus image = IJ.openImage("/home/eugene/Desktop/projects/IJROIsToSVG/gen_art.tif");
		image.show();
		RoiManager rm = RoiManager.getInstance2();
		if (rm == null) {
		    rm = new RoiManager(); // creates a new one if needed
		}
		rm.open( "/home/eugene/Desktop/projects/IJROIsToSVG/RoiSet2.zip" );
		//rm.open( "/home/eugene/Desktop/projects/IJROIsToSVG/gen_art_RoiSet.zip" );
		// run the plugin
		IJ.runPlugIn(IJRoiToSVG.class.getName(), "");
	}



}
