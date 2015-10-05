package org.osm2world.core.target.sdf;

import static java.awt.Color.WHITE;
import static java.lang.Math.max;
import static java.util.Collections.nCopies;
import static org.osm2world.core.target.common.material.Material.multiplyColor;

import java.awt.Color;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapAreaSegment;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXYZWithNormals;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.osm.data.OSMElement;
import org.osm2world.core.target.common.FaceTarget;
import org.osm2world.core.target.common.TextureData;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.world.data.WorldObject;

import org.osm2world.core.world.modules.BuildingModule.Building;
import org.osm2world.core.world.modules.BuildingModule.BuildingPart;
import org.osm2world.core.world.modules.BuildingModule.BuildingPart.Roof;
import org.osm2world.core.world.modules.RoadModule.Road;
import org.osm2world.core.world.modules.TreeModule.Tree;
import org.osm2world.core.world.modules.StreetFurnitureModule.StreetLamp;

import static java.lang.Math.*;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.SimplePolygonXZ;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class SdfTarget extends FaceTarget<RenderableToSdf> {

	private final Document sdfDoc;
	private final Node world;
    private Element elem;
    private Element link;
    private int visualIdx;
    private int streetLampIdx;
	private final PrintStream mtlStream;

	private final Map<VectorXYZ, Integer> vertexIndexMap = new HashMap<VectorXYZ, Integer>();
	private final Map<VectorXYZ, Integer> normalsIndexMap = new HashMap<VectorXYZ, Integer>();
	private final Map<VectorXZ, Integer> texCoordsIndexMap = new HashMap<VectorXZ, Integer>();
	private final Map<Material, String> materialMap = new HashMap<Material, String>();

	private int anonymousWOCounter = 0;

	private Material currentMaterial = null;
	private int currentMaterialLayer = 0;
	private static int anonymousMaterialCounter = 0;

	// this is approximatly one millimeter
	private static final double SMALL_OFFSET = 1e-3;

	public SdfTarget(Document sdfDoc, PrintStream mtlStream) {

		this.sdfDoc = sdfDoc;
		this.world = sdfDoc.getElementsByTagName("world").item(0);
		this.mtlStream = mtlStream;
        this.streetLampIdx = 0;

	}

	@Override
	public Class<RenderableToSdf> getRenderableType() {
		return RenderableToSdf.class;
	}

	@Override
	public void render(RenderableToSdf renderable) {
		renderable.renderTo(this);
	}

	@Override
	public boolean reconstructFaces() {
		return config != null && config.getBoolean("reconstructFaces", false);
	}

	@Override
	public void beginObject(WorldObject object) {

		if (object != null) {

			/* start an object with the object's class
			 * and the underlying OSM element's name/ref tags */

			MapElement element = object.getPrimaryMapElement();
			OSMElement osmElement;
			if (element instanceof MapNode) {
				osmElement = ((MapNode) element).getOsmNode();
			} else if (element instanceof MapWaySegment) {
				osmElement = ((MapWaySegment) element).getOsmWay();
			} else if (element instanceof MapArea) {
				osmElement = ((MapArea) element).getOsmObject();
			} else {
				osmElement = null;
			}

            String name = object.getClass().getSimpleName();
			if (osmElement != null && osmElement.tags.containsKey("name")) {
				name += " " + osmElement.tags.getValue("name");
			} else if (osmElement != null && osmElement.tags.containsKey("ref")) {
				name += " " + osmElement.tags.getValue("ref");
			} else {
				name += " " + anonymousWOCounter++;
			}

            link = null;
            if (object instanceof Road) {
                Road road = (Road) object;

                elem = sdfDoc.createElement("road");
		        world.appendChild(elem);

                Element width = sdfDoc.createElement("width");
                width.appendChild(sdfDoc.createTextNode(Double.toString(road.getWidth())));
		        elem.appendChild(width);

				for (VectorXZ v : road.getCenterlineXZ()) {
                    Element point = sdfDoc.createElement("point");
                    point.appendChild(sdfDoc.createTextNode(formatVector3D(v)));
		            elem.appendChild(point);
                }
            } else if (object instanceof Tree) {
                Tree tree = (Tree) object;

                elem = sdfDoc.createElement("model");
		        world.appendChild(elem);

                Element staticElem = sdfDoc.createElement("static");
                staticElem.appendChild(sdfDoc.createTextNode("true"));
		        elem.appendChild(staticElem);

                Element pose = sdfDoc.createElement("pose");
                pose.appendChild(sdfDoc.createTextNode(formatVector3D(tree.getPos()) + " 0 0 0"));
		        elem.appendChild(pose);

                Element link = sdfDoc.createElement("link");
		        elem.appendChild(link);

		        Attr attr = sdfDoc.createAttribute("name");
		        attr.setValue(name);
		        link.setAttributeNode(attr);

                Element visual = sdfDoc.createElement("visual");
	            link.appendChild(visual);

	            attr = sdfDoc.createAttribute("name");
		        attr.setValue(name);
	            visual.setAttributeNode(attr);

                Element geometry = sdfDoc.createElement("geometry");
	            visual.appendChild(geometry);

                Element mesh = sdfDoc.createElement("mesh");
	            geometry.appendChild(mesh);

                Element uri = sdfDoc.createElement("uri");
                uri.appendChild(sdfDoc.createTextNode("file://media/models/tree.dae"));
	            mesh.appendChild(uri);

            } else if (object instanceof StreetLamp) {
                StreetLamp lamp = (StreetLamp) object;

                Element include = sdfDoc.createElement("include");
		        world.appendChild(include);

                Element uri = sdfDoc.createElement("uri");
                uri.appendChild(sdfDoc.createTextNode("model://lamp_post"));
		        include.appendChild(uri);

                Element nameElem = sdfDoc.createElement("name");
                nameElem.appendChild(sdfDoc.createTextNode("Lamp Post " + streetLampIdx++));
		        include.appendChild(nameElem);

                Element staticElem = sdfDoc.createElement("static");
                staticElem.appendChild(sdfDoc.createTextNode("true"));
		        include.appendChild(staticElem);

                Element pose = sdfDoc.createElement("pose");
                pose.appendChild(sdfDoc.createTextNode(formatVector3D(lamp.getPos()) + " 0 0 0"));
		        include.appendChild(pose);
            } else if (object instanceof Building) {
                Building building = (Building) object;

                elem = sdfDoc.createElement("model");
		        world.appendChild(elem);

                Element staticElem = sdfDoc.createElement("static");
                staticElem.appendChild(sdfDoc.createTextNode("true"));
		        elem.appendChild(staticElem);

                Element pose = sdfDoc.createElement("pose");
                pose.appendChild(sdfDoc.createTextNode("0 0 0 0 0 0"));
		        elem.appendChild(pose);

                link = sdfDoc.createElement("link");
		        elem.appendChild(link);

		        Attr attr = sdfDoc.createAttribute("name");
		        attr.setValue(name);
		        link.setAttributeNode(attr);

                visualIdx = 0;
            } else {
                elem = null;
            }

            if (elem != null) {
		        Attr attr = sdfDoc.createAttribute("name");
		        attr.setValue(name);
		        elem.setAttributeNode(attr);
            }
		}

	}

    @Override
	public void drawTriangleStrip(Material material, List<VectorXYZ> vs,
			List<List<VectorXZ>> texCoordLists) {

    	for (VectorXYZ vector : vs) {
    		performNaNCheck(vector);
    	}

        if (link != null) {
            Element visual = sdfDoc.createElement("visual");
	        link.appendChild(visual);

	        Attr attr = sdfDoc.createAttribute("name");
		    attr.setValue(link.getAttributes().getNamedItem("name").getNodeValue() + " " + (visualIdx++));
	        visual.setAttributeNode(attr);

            Element pose = sdfDoc.createElement("pose");
            pose.appendChild(sdfDoc.createTextNode("0 0 " + Double.toString(vs.get(1).y)));
	        visual.appendChild(pose);

            Element geometry = sdfDoc.createElement("geometry");
	        visual.appendChild(geometry);

            Element polyline = sdfDoc.createElement("polyline");
	        geometry.appendChild(polyline);

            Element height = sdfDoc.createElement("height");
            height.appendChild(sdfDoc.createTextNode(Double.toString(vs.get(0).y - vs.get(1).y)));
	        polyline.appendChild(height);

            int i = 0;
    	    for (VectorXYZ vector : vs) {
                if (i++ % 2 == 1)
                    continue;
                Element point = sdfDoc.createElement("point");
                point.appendChild(sdfDoc.createTextNode(formatVector2D(vector)));
	            polyline.appendChild(point);
            }
        }
    }

	private void performNaNCheck(VectorXYZ v) {
		if (Double.isNaN(v.x) || Double.isNaN(v.y) || Double.isNaN(v.z)) {
			throw new IllegalArgumentException("NaN vector " + v.x + ", " + v.y + ", " + v.z);
		}
	}

	@Override
	public void drawFace(Material material, List<VectorXYZ> vs,
			List<VectorXYZ> normals, List<List<VectorXZ>> texCoordLists) {

		int[] normalIndices = null;
		if (normals != null) {
			normalIndices = normalsToIndices(normals);
		}

		VectorXYZ faceNormal = new TriangleXYZ(vs.get(0), vs.get(1), vs.get(2)).getNormal();

		for (int layer = 0; layer < max(1, material.getNumTextureLayers()); layer++) {

			useMaterial(material, layer);

			int[] texCoordIndices = null;
			if (texCoordLists != null && !texCoordLists.isEmpty()) {
				texCoordIndices = texCoordsToIndices(texCoordLists.get(layer));
			}

			writeFace(verticesToIndices((layer == 0)? vs : offsetVertices(vs, nCopies(vs.size(), faceNormal), layer * SMALL_OFFSET)),
					normalIndices, texCoordIndices);
		}
	}

	@Override
	public void drawTrianglesWithNormals(Material material,
			Collection<? extends TriangleXYZWithNormals> triangles,
			List<List<VectorXZ>> texCoordLists) {

		for (int layer = 0; layer < max(1, material.getNumTextureLayers()); layer++) {

			useMaterial(material, layer);

			int triangleNumber = 0;
			for (TriangleXYZWithNormals t : triangles) {

				int[] texCoordIndices = null;
				if (texCoordLists != null && !texCoordLists.isEmpty()) {
					List<VectorXZ> texCoords = texCoordLists.get(layer);
					texCoordIndices = texCoordsToIndices(
							texCoords.subList(3*triangleNumber, 3*triangleNumber + 3));
				}

				writeFace(verticesToIndices((layer == 0)? t.getVertices() : offsetVertices(t.getVertices(), t.getNormals(), layer * SMALL_OFFSET)),
						normalsToIndices(t.getNormals()), texCoordIndices);

				triangleNumber ++;
			}

		}

	}

	private void useMaterial(Material material, int layer) {
		if (!material.equals(currentMaterial) || (layer != currentMaterialLayer)) {

			String name = materialMap.get(material);
			if (name == null) {
				name = Materials.getUniqueName(material);
				if (name == null) {
					name = "MAT_" + anonymousMaterialCounter;
					anonymousMaterialCounter += 1;
				}
				materialMap.put(material, name);
				writeMaterial(material, name);
			}

			//sdfStream.println("usemtl " + name + "_" + layer);

			currentMaterial = material;
			currentMaterialLayer = layer;
		}
	}

	private List<? extends VectorXYZ> offsetVertices(List<? extends VectorXYZ> vs, List<VectorXYZ> directions, double offset) {

		List<VectorXYZ> result = new ArrayList<VectorXYZ>(vs.size());

		for (int i = 0; i < vs.size(); i++) {
			result.add(vs.get(i).add(directions.get(i).mult(offset)));
		}

		return result;
	}

	private int[] verticesToIndices(List<? extends VectorXYZ> vs) {
		return vectorsToIndices(vertexIndexMap, "v ", vs);
	}

	private int[] normalsToIndices(List<? extends VectorXYZ> normals) {
		return vectorsToIndices(normalsIndexMap, "vn ", normals);
	}

	private int[] texCoordsToIndices(List<VectorXZ> texCoords) {
		return vectorsToIndices(texCoordsIndexMap, "vt ", texCoords);
	}

	private <V> int[] vectorsToIndices(Map<V, Integer> indexMap,
			String sdfLineStart, List<? extends V> vectors) {

		int[] indices = new int[vectors.size()];

		for (int i=0; i<vectors.size(); i++) {
			final V v = vectors.get(i);
			Integer index = indexMap.get(v);
			if (index == null) {
				index = indexMap.size();
                //if (sdfLineStart == "v " && elem != null) {
                //    Element point = sdfDoc.createElement("point");
                //    point.appendChild(sdfDoc.createTextNode(formatVector(v)));
		        //    elem.appendChild(point);
                //}
				//sdfStream.println(sdfLineStart + " " + formatVector(v));
				indexMap.put(v, index);
			}
			indices[i] = index;
		}

		return indices;

	}

	private String formatVector2D(Object v) {

		if (v instanceof VectorXYZ) {
			VectorXYZ vXYZ = (VectorXYZ)v;
			return vXYZ.x + " " + vXYZ.z;
		} else {
			VectorXZ vXZ = (VectorXZ)v;
			return vXZ.x + " " + vXZ.z;
		}

	}

	private String formatVector3D(Object v) {

		if (v instanceof VectorXYZ) {
			VectorXYZ vXYZ = (VectorXYZ)v;
			return vXYZ.x + " " + vXYZ.z + " " + vXYZ.y;
		} else {
			VectorXZ vXZ = (VectorXZ)v;
			return vXZ.x + " " + vXZ.z + " 0.0";
		}

	}

	private void writeFace(int[] vertexIndices, int[] normalIndices,
			int[] texCoordIndices) {

		assert normalIndices == null
				|| vertexIndices.length == normalIndices.length;

		//sdfStream.print("f");

		//for (int i = 0; i < vertexIndices.length; i++) {

		//	sdfStream.print(" " + (vertexIndices[i]+1));

		//	if (texCoordIndices != null && normalIndices == null) {
		//		sdfStream.print("/" + (texCoordIndices[i]+1));
		//	} else if (texCoordIndices == null && normalIndices != null) {
		//		sdfStream.print("//" + (normalIndices[i]+1));
		//	} else if (texCoordIndices != null && normalIndices != null) {
		//		sdfStream.print("/" + (texCoordIndices[i]+1)
		//				+ "/" + (normalIndices[i]+1));
		//	}

		//}

		//sdfStream.println();
	}

	private void writeMaterial(Material material, String name) {

		for (int i = 0; i < max(1, material.getNumTextureLayers()); i++) {

			TextureData textureData = null;
			if (material.getNumTextureLayers() > 0) {
				textureData = material.getTextureDataList().get(i);
			}

			mtlStream.println("newmtl " + name + "_" + i);

			if (textureData == null || textureData.colorable) {
				writeColorLine("Ka", material.ambientColor());
				writeColorLine("Kd", material.diffuseColor());
				//Ks
				//Ns
			} else {
				writeColorLine("Ka", multiplyColor(WHITE, material.getAmbientFactor()));
				writeColorLine("Kd", multiplyColor(WHITE, 1 - material.getAmbientFactor()));
				//Ks
				//Ns
			}

			if (textureData != null) {
				mtlStream.println("map_Ka " + textureData.file);
				mtlStream.println("map_Kd " + textureData.file);
			}
			mtlStream.println();
		}
	}

	private void writeColorLine(String lineStart, Color color) {

		mtlStream.println(lineStart
				+ " " + color.getRed() / 255f
				+ " " + color.getGreen() / 255f
				+ " " + color.getBlue() / 255f);

	}

}
