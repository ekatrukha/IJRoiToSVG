ImageJ ROIs to SVG converter
===

This is a simple [ImageJ](https://imagej.net/ij/)/[FIJI](https://fiji.sc/) plugin that takes all ROIs in ROI manager and converts them to a single [SVG](https://en.wikipedia.org/wiki/SVG) file.  
It maps rectangle/oval/polygons/freehand path ROIs to corresponding SVG primitives,   
preserving ROI name, fill color, stroke width (color). 

### Installaton

Download and copy the _jar_ file from the latest [release](https://github.com/ekatrukha/IJRoiToSVG/releases) to _plugins_ folder of your ImageJ/FIJI.   
The plugin should appear as _Plugins -> ROIs to SVG_ menu item.   
Or search for this name in _Plugins -> Utilities -> Find Commands..._

### Usage

Before running the plugin, make sure you have at least one ROI in the [ROI Manager](https://imagej.net/ij/docs/menus/analyze.html#manager).  
Optionally, you can have an image open.    
After the plugin starts, the following dialog appears:   
<img width="347" height="433" alt="parameters dialog" src="https://github.com/user-attachments/assets/c7fbabb1-f432-4972-80f2-a27e8bb6d2b8" />
 

The parameters are:
* **Area**, the input exported area that would be mapped to the final output canvas. It can be set to the current image or to the bounding boxes of all ROIs.
* **Output canvas width and height** define the size of the output SVG canvas;
* **Output units** are the units of the canvas size;
* **Border around** (in units) specifies if you want a border around the exported area;
* **Center output** checkbox, if selected, the area will be centered. If not, it will be in the top-left corner.
* **Set output A4 landscape/portrait** buttons for convenience will set the canvas size and units in the dialog.

Once you click OK, an SVG file saving dialog should appear.    
You can open and see the output in [Inkscape](https://inkscape.org/), for example.   
That's it!

---------- 

Developed in [Cell Biology group](http://cellbiology.science.uu.nl) of Utrecht University.  
<a href="mailto:katpyxa@gmail.com">E-mail</a> for any questions or tag <a href="https://forum.image.sc/u/ekatrukha/summary">@ekatrukha</a> at <a href="https://forum.image.sc/">image.sc</a> forum.   
