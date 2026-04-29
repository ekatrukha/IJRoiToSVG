package ijroitosvg;

import java.awt.Color;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.TextField;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.gui.OvalRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.io.SaveDialog;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;

public class IJRoiToSVG implements PlugIn 
{

	String sUnits = Prefs.get( "IJRoiToSVG.sUnits", "mm" );
	
	float fBorder = ( float ) Prefs.get( "IJRoiToSVG.fBorder",  10.0); 
	
	int canvasW = ( int ) Prefs.get( "IJRoiToSVG.canvasW",  297.0);
	
	int canvasH = ( int ) Prefs.get( "IJRoiToSVG.canvasH",  210.0);
	
	boolean bCentered = Prefs.get( "IJRoiToSVG.bCentered",  true); 
	
	boolean bImagePresent = true;
	
	boolean bUseImage = false;
	
	float fShiftX = 0;
	
	float fShiftY = 0;
	
	float fScaleFactor = 1.0f;

	@Override
	public void run( String arg )
	{

		RoiManager rm = RoiManager.getInstance();
		if (rm == null) 
		{
			IJ.showMessage( "No ROIs in ROI manager." );
			return;
		}
		else if(rm.getCount() == 0)
		{
			IJ.showMessage( "No ROIs in ROI manager." );
			return;			
		}
		final ImagePlus imp = IJ.getImage();
		
		if (imp == null)
		{
			bImagePresent = false;
			bUseImage = false;
		}
		final GenericDialog gdParams = new GenericDialog( "ROIs to SVG converter" );
		
		final String [] inputSize = new String[] {"ROIs boinding box", "Current image"};
		if(bImagePresent)
		{	
			gdParams.addChoice( "Area", inputSize,  Prefs.get( "IJRoiToSVG.inputSize", inputSize[0]) );
		}
		gdParams.addMessage( "Output canvas size:" );
		gdParams.addNumericField( "width: ", canvasW );
		gdParams.addNumericField( "height: ", canvasH );
		gdParams.addStringField( "Output units", sUnits );
		gdParams.addNumericField( "Border around: ", fBorder );
		final TextField nfWidth  = (TextField) gdParams.getNumericFields().get( 0 );
		final TextField nfHeight = (TextField) gdParams.getNumericFields().get( 1 );

		final TextField tfUnits  = (TextField) gdParams.getStringFields().get( 0 );
		gdParams.addButton( "Set output A4 landscape", (e)->
		{
			nfWidth.setText( "297");
			nfHeight.setText( "210");
			tfUnits.setText( "mm" );
		} );
		gdParams.addButton( "Set output A4 portrait", (e) ->
		{
			nfWidth.setText( "210");
			nfHeight.setText( "297");
			tfUnits.setText( "mm" );	
		});
		gdParams.addCheckbox( "Center output" , bCentered );
		gdParams.pack();
		gdParams.showDialog();
		
		if ( gdParams.wasCanceled() )
			return;
		
		if(bImagePresent)
		{
			int inputN = gdParams.getNextChoiceIndex();
			Prefs.set( "IJRoiToSVG.inputSize", inputSize[inputN]);
			if(inputN == 1)
			{
				bUseImage = true;
			}
		}	
		canvasW = (int) Math.round( gdParams.getNextNumber());
		Prefs.set( "IJRoiToSVG.canvasW",  (double)canvasW);
		
		canvasH = (int) Math.round( gdParams.getNextNumber());
		Prefs.set( "IJRoiToSVG.canvasH",  (double)canvasH);
		
		sUnits = gdParams.getNextString();
		Prefs.get( "IJRoiToSVG.sUnits", sUnits );
		
		fBorder = ( float ) gdParams.getNextNumber();
		Prefs.set( "IJRoiToSVG.fBorder",  fBorder);
		
		bCentered = gdParams.getNextBoolean();
		Prefs.get( "IJRoiToSVG.bCentered",  bCentered); 
		
		int nWOut = 1;
		int nHOut = 1;
		
		Rectangle bounds = new Rectangle (0,0,1,1);
		if(bUseImage && imp != null)
		{
			nWOut = imp.getWidth();
			nHOut = imp.getHeight();
			bounds = new Rectangle (0, 0, nWOut, nHOut);
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
			bounds = estimateAllROIsBounds(rois);
			nWOut = bounds.width;
			nHOut = bounds.height;		
		}
		
		//calculate scaling factor
		fScaleFactor = Math.min((canvasW - 2.0f * fBorder)/nWOut, (canvasH - 2.0f * fBorder)/nHOut);
		
		fShiftX = fBorder - bounds.x * fScaleFactor;
		fShiftY = fBorder - bounds.y * fScaleFactor;
		
		if(bCentered)
		{
			fShiftX += 0.5f * (canvasW - (nWOut * fScaleFactor + 2.0f * fBorder) ); 
			fShiftY += 0.5f * (canvasH - (nHOut * fScaleFactor + 2.0f * fBorder) ); 
		}

		String out = exportSvg( rois, canvasW, canvasH);

		try 
		{
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

	    for (Roi roi : rois) 
	    {
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
	        b.x * fScaleFactor + fShiftX, 
	        b.y * fScaleFactor + fShiftY, 
	        b.width  * fScaleFactor, 
	        b.height * fScaleFactor
	    );
	}
	
	String polygonToSvg(final PolygonRoi roi) 
	{
	    Polygon p = roi.getPolygon();

	    StringBuilder points = new StringBuilder();
	    for (int i = 0; i < p.npoints; i++) {
	        points.append(String.format("%.2f,",p.xpoints[i] * fScaleFactor + fShiftX))
	        	  .append(String.format("%.2f ",p.ypoints[i] * fScaleFactor + fShiftY));
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

	    double cx = ( b.x + b.width  / 2.0 ) *  fScaleFactor + fShiftX;
	    double cy = ( b.y + b.height / 2.0 ) *  fScaleFactor + fShiftY;
	    double rx = b.width / 2.0   * fScaleFactor;
	    double ry = b.height / 2.0  * fScaleFactor;

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
	            d.append(String.format("M %.2f %.2f ", fp.xpoints[i] * fScaleFactor + fShiftX, fp.ypoints[i] * fScaleFactor + fShiftY));
	        } 
	        else 
	        {
	            d.append(String.format("L %.2f %.2f ", fp.xpoints[i] * fScaleFactor + fShiftX, fp.ypoints[i] * fScaleFactor + fShiftY));
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
    	int nXmax = 0;
    	int nYmax = 0;
    	int nXmin = Integer.MAX_VALUE;
    	int nYmin = Integer.MAX_VALUE;

    	for (Roi roi : rois) 
    	{
    		final Rectangle rect = roi.getBounds();
    		nXmax = Math.max( nXmax, rect.x + rect.width );
    		nYmax = Math.max( nYmax, rect.y + rect.height );
    		nXmin = Math.min( nXmin, rect.x );
    		nYmin = Math.min( nYmin, rect.y );
    	}
    	return new Rectangle(nXmin, nYmin, nXmax - nXmin, nYmax - nYmin);
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
		//ImagePlus image = IJ.openImage("/home/eugene/Desktop/projects/IJROIsToSVG/test.tif");
		ImagePlus image = IJ.openImage("/home/eugene/Desktop/projects/IJROIsToSVG/gen_art.tif");
		image.show();
		RoiManager rm = RoiManager.getInstance2();
		if (rm == null) {
		    rm = new RoiManager(); // creates a new one if needed
		}
		//rm.open( "/home/eugene/Desktop/projects/IJROIsToSVG/RoiSet2.zip" );
		rm.open( "/home/eugene/Desktop/projects/IJROIsToSVG/gen_art_RoiSet.zip" );
		// run the plugin
		IJ.runPlugIn(IJRoiToSVG.class.getName(), "");
	}



}
