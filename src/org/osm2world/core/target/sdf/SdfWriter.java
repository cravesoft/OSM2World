package org.osm2world.core.target.sdf;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;

import org.osm2world.core.GlobalValues;
import org.osm2world.core.map_data.creation.MapProjection;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.TargetUtil;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.OutputKeys;

import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * utility class for creating an Gazebo SDF file
 */
public final class SdfWriter {

	/** prevents instantiation */
	private SdfWriter() { }

	public static final void writeSdfFile(
			File sdfFile, MapData mapData,
			MapProjection mapProjection,
			Camera camera, Projection projection)
			throws IOException {

		try {

		    File mtlFile = new File(sdfFile.getAbsoluteFile() + ".material");
		    if (!mtlFile.exists()) {
		    	mtlFile.createNewFile();
		    }

		    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

		    PrintStream mtlStream = new PrintStream(mtlFile);

		    /* write comments at the beginning of both files */

		    writeSdfHeader(doc, mapProjection);

		    writeMtlHeader(mtlStream);

		    /* write actual file content */

		    SdfTarget target = new SdfTarget(doc, mtlStream);

		    TargetUtil.renderWorldObjects(target, mapData, true);

            /* write the content into xml file */
		    TransformerFactory transformerFactory = TransformerFactory.newInstance();
		    Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		    DOMSource source = new DOMSource(doc);
		    StreamResult result = new StreamResult(sdfFile);

            transformer.transform(source, result);
		    mtlStream.close();

		} catch (ParserConfigurationException e) {
			throw new IOException(e);
		} catch (TransformerException  e) {
			throw new IOException(e);
        }
	}

	public static final void writeSdfFiles(
			final File sdfDirectory, MapData mapData,
			final MapProjection mapProjection,
			Camera camera, Projection projection,
			int primitiveThresholdPerFile)
			throws IOException {

        try {
		    if (!sdfDirectory.exists()) {
		    	sdfDirectory.mkdir();
		    }

		    checkArgument(sdfDirectory.isDirectory());

		    final File mtlFile = new File(sdfDirectory.getPath()
		    		+ File.separator + "materials.material");
		    if (!mtlFile.exists()) {
		    	mtlFile.createNewFile();
		    }

		    final PrintStream mtlStream = new PrintStream(mtlFile);

		    writeMtlHeader(mtlStream);

		    /* create iterator which creates and wraps .sdf files as needed */

		    Iterator<SdfTarget> sdfIterator = new Iterator<SdfTarget>() {

		    	private int fileCounter = 0;
		        private DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		        private DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		        private TransformerFactory transformerFactory = TransformerFactory.newInstance();
		        private Transformer transformer = transformerFactory.newTransformer();
                Document doc = null;
		    	File sdfFile = null;

		    	@Override
		    	public boolean hasNext() {
		    		return true;
		    	}

		    	@Override
		    	public SdfTarget next() {

		    		try {
		    			if (doc != null) {
                            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		                    DOMSource source = new DOMSource(doc);
		                    StreamResult result = new StreamResult(sdfFile);

                            transformer.transform(source, result);
		                    mtlStream.close();
		    				fileCounter ++;
		    			}

		    			sdfFile = new File(sdfDirectory.getPath() + File.separator
		    					+ "part" + format("%04d", fileCounter) + ".sdf");

                        doc = docBuilder.newDocument();

		                PrintStream mtlStream = new PrintStream(mtlFile);

		    			writeSdfHeader(doc, mapProjection);

		    			return new SdfTarget(doc, mtlStream);

		    		} catch (FileNotFoundException e) {
		    			throw new RuntimeException(e);
		    		} catch (IOException e) {
		    			throw new RuntimeException(e);
		    		} catch (TransformerException  e) {
		    			throw new RuntimeException(e);
		    		}

		    	}

		    	@Override
		    	public void remove() {
		    		throw new UnsupportedOperationException();
		    	}

		    };

		    /* write file content */

		    TargetUtil.renderWorldObjects(sdfIterator, mapData, primitiveThresholdPerFile);

		    mtlStream.close();

		} catch (ParserConfigurationException e) {
			throw new IOException(e);
		} catch (TransformerException  e) {
			throw new IOException(e);
        }
	}

	private static final void writeSdfHeader(Document doc,
			MapProjection mapProjection) {

		Comment comment = doc.createComment("This file was created by OSM2World "
				+ GlobalValues.VERSION_STRING + " - "
				+ GlobalValues.OSM2WORLD_URI + "\n"
		        + "Projection information:\n"
		        + "Coordinate origin (0,0,0): "
				+ "lat " + mapProjection.calcLat(VectorXZ.NULL_VECTOR) + ", "
				+ "lon " + mapProjection.calcLon(VectorXZ.NULL_VECTOR) + ", "
				+ "ele 0\n"
		        + "North direction: " + new VectorXYZ(
						mapProjection.getNorthUnit().x, 0,
						- mapProjection.getNorthUnit().z) + "\n"
		        + "1 coordinate unit corresponds to roughly "
				+ "1 m in reality\n");
        doc.appendChild(comment);

        Element sdf = doc.createElement("sdf");
		doc.appendChild(sdf);

		Attr attr = doc.createAttribute("version");
		attr.setValue("1.5");
		sdf.setAttributeNode(attr);

        Element world = doc.createElement("world");
		sdf.appendChild(world);

		attr = doc.createAttribute("name");
		attr.setValue("default");
		world.setAttributeNode(attr);

        Element coords = doc.createElement("spherical_coordinates");
		world.appendChild(coords);

        Element surfaceModel = doc.createElement("surface_model");
        surfaceModel.appendChild(doc.createTextNode("EARTH_WGS84"));
		coords.appendChild(surfaceModel);

        Element latitude = doc.createElement("latitude_deg");
        latitude.appendChild(doc.createTextNode(Double.toString(mapProjection.calcLat(VectorXZ.NULL_VECTOR))));
		coords.appendChild(latitude);

        Element longitude = doc.createElement("longitude_deg");
        longitude.appendChild(doc.createTextNode(Double.toString(mapProjection.calcLon(VectorXZ.NULL_VECTOR))));
		coords.appendChild(longitude);

        Element elevation = doc.createElement("elevation");
        elevation.appendChild(doc.createTextNode("0.0"));
		coords.appendChild(elevation);

        Element headingDeg = doc.createElement("heading_deg");
        headingDeg.appendChild(doc.createTextNode("0"));
		coords.appendChild(headingDeg);

        Element include = doc.createElement("include");
		world.appendChild(include);

        Element uri = doc.createElement("uri");
        uri.appendChild(doc.createTextNode("model://sun"));
		include.appendChild(uri);
    }

	private static final void writeMtlHeader(PrintStream mtlStream) {

		mtlStream.println("// This file was created by OSM2World "
				+ GlobalValues.VERSION_STRING + " - "
				+ GlobalValues.OSM2WORLD_URI + "\n");

	}

}
