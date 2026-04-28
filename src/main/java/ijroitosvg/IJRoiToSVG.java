

package ijroitosvg;

import java.awt.Polygon;
import java.awt.Rectangle;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;

public class IJRoiToSVG implements PlugIn 
{
	
	boolean bSetStrokeWidth = false;
	
	float fStrokeWidth = 1;
	
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
		final Roi[] rois = rm.getRoisAsArray();
		
		String out = exportSvg( rois, imp.getWidth(), imp.getHeight());
	}
	
	String exportSvg(Roi[] rois, int width, int height) 
	{
	    StringBuilder sb = new StringBuilder();

	    sb.append(String.format(
	        "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\">\n",
	        width, height
	    ));
	    
	    if(bSetStrokeWidth)
	    {
	    	sStrokeAdd = " stroke-width=\""+ Float.toString( fStrokeWidth )+"\"";
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
	        "<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" " +
	        "fill=\"none\" stroke=\"black\"" + sStrokeAdd + "/>",
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
	        "<polygon points=\"%s\" fill=\"none\" stroke=\"black\"" + sStrokeAdd + "/>",
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
	        "<ellipse cx=\"%.2f\" cy=\"%.2f\" rx=\"%.2f\" ry=\"%.2f\" " +
	        "fill=\"none\" stroke=\"black\"" + sStrokeAdd + "/>",
	        cx, cy, rx, ry
	    );
	}
	
	String pathToSvg(Roi roi) 
	{
	    FloatPolygon fp = roi.getFloatPolygon();

	    StringBuilder d = new StringBuilder();

	    for (int i = 0; i < fp.npoints; i++) {
	        if (i == 0) {
	            d.append(String.format("M %.2f %.2f ", fp.xpoints[i], fp.ypoints[i]));
	        } else {
	            d.append(String.format("L %.2f %.2f ", fp.xpoints[i], fp.ypoints[i]));
	        }
	    }

	    if (roi.isArea()) {
	        d.append("Z");
	    }

	    return String.format(
	        "<path d=\"%s\" fill=\"none\" stroke=\"black\"" + sStrokeAdd + "/>",
	        d.toString()
	    );
	}

	/**
	 * Main method for debugging.
	 *
	 * For debugging, it is convenient to have a method that starts ImageJ, loads
	 * an image and calls the plugin, e.g. after setting breakpoints.
	 *
	 * @param args unused
	 */
	public static void main(String[] args) throws Exception {

	}



}
