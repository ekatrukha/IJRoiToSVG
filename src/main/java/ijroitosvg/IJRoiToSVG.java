package ijroitosvg;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import ij.IJ;
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
	
	boolean bSetStrokeWidth = false;
	
	float fStrokeWidth = 0.2f;
	
	String sStrokeAdd = "";

	@Override
	public void run( String arg )
	{
		final ImagePlus imp = IJ.getImage();

		if (imp == null)
		{
			IJ.noImage();
			return;
		}
		RoiManager rm = RoiManager.getInstance();
		if (rm == null) 
		{
			IJ.showMessage( "ROIs in ROI manager." );
			return;
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

		String out = exportSvg( rois, imp.getWidth(), imp.getHeight());

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
	        "<svg \nxmlns=\"http://www.w3.org/2000/svg\" \nwidth=\"%d\" \nheight=\"%d\">\n",
	        width, height
	    ));
	    
	    if(bSetStrokeWidth)
	    {
	    	sStrokeAdd = " \nstroke-width=\""+ Float.toString( fStrokeWidth )+"mm\"";
	    }
	    for (Roi roi : rois) {
	        sb.append("  ").append(roiToSvg(roi)).append("\n");
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
	
	String rectToSvg(Roi r) 
	{
	    Rectangle b = r.getBounds();
	    return String.format(
	        "<rect \nx=\"%d\" \ny=\"%d\" \nwidth=\"%d\" \nheight=\"%d\" " +
	        "\nfill=\"none\" \nstroke=\"black\"" + sStrokeAdd + "/>",
	        b.x, b.y, b.width, b.height
	    );
	}
	
	String polygonToSvg(PolygonRoi r) 
	{
	    Polygon p = r.getPolygon();

	    StringBuilder points = new StringBuilder();
	    for (int i = 0; i < p.npoints; i++) {
	        points.append(p.xpoints[i])
	              .append(",")
	              .append(p.ypoints[i])
	              .append(" ");
	    }

	    return String.format(
	        "<polygon \npoints=\"%s\" \nfill=\"none\" \nstroke=\"black\"" + sStrokeAdd + "/>",
	        points.toString().trim()
	    );
	}
	
	String ovalToSvg(OvalRoi r) 
	{
	    Rectangle b = r.getBounds();

	    double cx = b.x + b.width / 2.0;
	    double cy = b.y + b.height / 2.0;
	    double rx = b.width / 2.0;
	    double ry = b.height / 2.0;

	    return String.format(
	        "<ellipse \ncx=\"%.2f\" \ncy=\"%.2f\" \nrx=\"%.2f\" \nry=\"%.2f\" " +
	        "\nfill=\"none\" \nstroke=\"black\"" + sStrokeAdd + "/>",
	        cx, cy, rx, ry
	    );
	}
	
	String pathToSvg(Roi roi) 
	{
	    FloatPolygon fp = roi.getFloatPolygon();

	    StringBuilder d = new StringBuilder();

	    for (int i = 0; i < fp.npoints; i++) 
	    {
	        if (i == 0) 
	        {
	            d.append(String.format("M %.2f %.2f ", fp.xpoints[i], fp.ypoints[i]));
	        } 
	        else 
	        {
	            d.append(String.format("L %.2f %.2f ", fp.xpoints[i], fp.ypoints[i]));
	        }
	    }

	    if (roi.isArea()) {
	        d.append("Z");
	    }

	    return String.format(
	        "<path \nd=\"%s\" \nfill=\"none\" \nstroke=\"black\"" + sStrokeAdd + "/>",
	        d.toString()
	    );
	}
	
    public static String getTimestamp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
        return LocalDateTime.now().format(formatter);
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

	}



}
