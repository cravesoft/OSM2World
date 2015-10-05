package org.osm2world.viewer.control.actions;

import java.awt.HeadlessException;
import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.osm2world.core.target.sdf.SdfWriter;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.MessageManager;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ViewerFrame;

public class ExportSdfDirAction extends AbstractExportAction {

	public ExportSdfDirAction(ViewerFrame viewerFrame, Data data,
			MessageManager messageManager, RenderOptions renderOptions) {

		super("Export SDF directory", viewerFrame, data, messageManager, renderOptions);
		putValue(SHORT_DESCRIPTION, "Writes several smaller Gazebo .sdf files to a directory");

	}

	@Override
	protected boolean chooseDirectory() {
		return true;
	}

	protected FileNameExtensionFilter getFileNameExtensionFilter() {
		return null;
	}

	@Override
	protected void performExport(File file) throws HeadlessException {

		try {

			String thresholdString = JOptionPane.showInputDialog(
					viewerFrame, "Graphics primitives per file", 10000);

			int primitiveThresholdPerFile = Integer.parseInt(thresholdString);

			/* write the file */

			SdfWriter.writeSdfFiles(
					file,
					data.getConversionResults().getMapData(),
					data.getConversionResults().getMapProjection(),
					null, renderOptions.projection,
					primitiveThresholdPerFile);

			messageManager.addMessage("exported Gazebo .sdf file " + file);

		} catch (IOException e) {
			JOptionPane.showMessageDialog(viewerFrame,
					e.toString(),
					"Could not export Gazebo .sdf file",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(viewerFrame,
					e.toString(),
					"please enter a valid number of primitives per file",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

}
