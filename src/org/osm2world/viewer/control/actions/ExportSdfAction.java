package org.osm2world.viewer.control.actions;

import java.awt.HeadlessException;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.osm2world.core.target.sdf.SdfWriter;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.MessageManager;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ViewerFrame;

public class ExportSdfAction extends AbstractExportAction {

	public ExportSdfAction(ViewerFrame viewerFrame, Data data,
			MessageManager messageManager, RenderOptions renderOptions) {

		super("Export SDF file", viewerFrame, data, messageManager, renderOptions);
		putValue(SHORT_DESCRIPTION, "Writes a Gazebo .sdf file");
		putValue(MNEMONIC_KEY, KeyEvent.VK_O);

	}

	protected FileNameExtensionFilter getFileNameExtensionFilter() {
		return new FileNameExtensionFilter("Gazebo .sdf files", "sdf");
	}

	@Override
	protected void performExport(File file) throws HeadlessException {

		try {

			/* write the file */

			SdfWriter.writeSdfFile(
					file,
					data.getConversionResults().getMapData(),
					data.getConversionResults().getMapProjection(),
					null, renderOptions.projection);

			messageManager.addMessage("exported Gazebo .sdf file " + file);

		} catch (IOException e) {
			JOptionPane.showMessageDialog(viewerFrame,
					e.toString(),
					"Could not export Gazebo .sdf file",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

}
