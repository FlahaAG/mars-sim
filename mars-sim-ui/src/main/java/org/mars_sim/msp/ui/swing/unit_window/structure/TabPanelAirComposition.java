/**
 * Mars Simulation Project
 * TabPanelTabPanelAirComposition.java
 * @version 3.1.0 2017-03-08
 * @author Manny Kung
 */
package org.mars_sim.msp.ui.swing.unit_window.structure;

import java.awt.BorderLayout;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.structure.CompositionOfAir;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;

import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.MarsPanelBorder;
import org.mars_sim.msp.ui.swing.NumberCellRenderer;
import org.mars_sim.msp.ui.swing.tool.ColumnResizer;

import org.mars_sim.msp.ui.swing.tool.TableStyle;
import org.mars_sim.msp.ui.swing.tool.ZebraJTable;
import org.mars_sim.msp.ui.swing.unit_window.TabPanel;

/**
 * This is a tab panel for displaying the composition of air of each inhabitable building in a settlement.
 */
public class TabPanelAirComposition
extends TabPanel {

	// default logger.
	//private static Logger logger = Logger.getLogger(TabPanelAirComposition.class.getName());

	// Data cache
	private int numBuildingsCache;
	private double o2Cache, cO2Cache, n2Cache, h2OCache, arCache, totalPressureCache;

	private List<Building> buildingsCache;

	private JLabel o2Label, cO2Label, n2Label, h2OLabel, arLabel, totalPressureLabel;

	private JTable table ;

	private JRadioButton percent_btn, pressure_btn, mass_btn, moles_btn, temperature_btn;
	
	private JScrollPane scrollPane;
	
	private JCheckBox checkbox;
	
	private ButtonGroup bG;
	
	private TableModel tableModel;

	private DecimalFormat fmt3 = new DecimalFormat("0.000");//Msg.getString("decimalFormat3")); //$NON-NLS-1$
	private DecimalFormat fmt2 = new DecimalFormat("0.00");//Msg.getString("decimalFormat2")); //$NON-NLS-1$
	private DecimalFormat fmt1 = new DecimalFormat("0.0");//Msg.getString("decimalFormat1")); //$NON-NLS-1$

	private Settlement settlement;
	private BuildingManager manager;
	private CompositionOfAir air;


	/**
	 * Constructor.
	 * @param unit the unit to display.
	 * @param desktop the main desktop.
	 */
	public TabPanelAirComposition(Unit unit, MainDesktopPane desktop) {

		// Use the TabPanel constructor
		super(
			Msg.getString("TabPanelAirComposition.title"), //$NON-NLS-1$
			null,
			Msg.getString("TabPanelAirComposition.tooltip"), //$NON-NLS-1$
			unit, desktop
		);

		settlement = (Settlement) unit;
		manager = settlement.getBuildingManager();
		air = settlement.getCompositionOfAir();

		buildingsCache = manager.getBuildingsWithLifeSupport();
		numBuildingsCache = buildingsCache.size();

		// Prepare heating System label panel.
		JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		topContentPanel.add(labelPanel);

		JLabel label = new JLabel(Msg.getString("TabPanelAirComposition.title"), JLabel.CENTER); //$NON-NLS-1$
		label.setFont(new Font("Serif", Font.BOLD, 16));
		labelPanel.add(label);

		// Prepare heat info panel.
		JPanel infoPanel = new JPanel(new GridLayout(9, 1, 0, 0));
		infoPanel.setBorder(new MarsPanelBorder());
		topContentPanel.add(infoPanel);

		totalPressureCache = settlement.getAirPressure()/1000D; // convert to kPascal by multiplying 1000
		totalPressureLabel = new JLabel(Msg.getString("TabPanelAirComposition.label.totalPressure", fmt2.format(totalPressureCache)), JLabel.CENTER); //$NON-NLS-1$
		infoPanel.add(totalPressureLabel);

		// add an empty label for separation
		infoPanel.add(new JLabel(""));

		// CO2, H2O, N2, O2, Others (Ar2, He, CH4...)

		infoPanel.add(new JLabel(Msg.getString("TabPanelAirComposition.label"), JLabel.CENTER)); //$NON-NLS-1$

		cO2Cache = getOverallComposition(0);
		cO2Label = new JLabel(Msg.getString("TabPanelAirComposition.label.cO2", fmt3.format(cO2Cache)), JLabel.CENTER); //$NON-NLS-1$
		infoPanel.add(cO2Label);

		arCache = getOverallComposition(1);
		arLabel = new JLabel(Msg.getString("TabPanelAirComposition.label.ar", fmt2.format(arCache)), JLabel.CENTER); //$NON-NLS-1$
		infoPanel.add(arLabel);
		
		n2Cache = getOverallComposition(2);
		n2Label = new JLabel(Msg.getString("TabPanelAirComposition.label.n2", fmt1.format(n2Cache)), JLabel.CENTER); //$NON-NLS-1$
		infoPanel.add(n2Label);

		o2Cache = getOverallComposition(3);
		o2Label = new JLabel(Msg.getString("TabPanelAirComposition.label.o2", fmt2.format(o2Cache)), JLabel.CENTER); //$NON-NLS-1$
		infoPanel.add(o2Label);

		h2OCache = getOverallComposition(4);
		h2OLabel = new JLabel(Msg.getString("TabPanelAirComposition.label.h2O", fmt2.format(h2OCache)), JLabel.CENTER); //$NON-NLS-1$
		infoPanel.add(h2OLabel);

		// add an empty label for separation
		infoPanel.add(new JLabel(""));

		// Create override check box panel.
		JPanel radioPane = new JPanel(new FlowLayout(FlowLayout.CENTER));
		topContentPanel.add(radioPane, BorderLayout.SOUTH);
		
	    percent_btn = new JRadioButton(Msg.getString("TabPanelAirComposition.checkbox.percent")); //$NON-NLS-1$
	    percent_btn.setToolTipText(Msg.getString("TabPanelAirComposition.checkbox.percent.tooltip")); //$NON-NLS-1$
	    pressure_btn = new JRadioButton(Msg.getString("TabPanelAirComposition.checkbox.pressure")); //$NON-NLS-1$
	    pressure_btn.setToolTipText(Msg.getString("TabPanelAirComposition.checkbox.pressure.tooltip")); //$NON-NLS-1$
	    mass_btn = new JRadioButton(Msg.getString("TabPanelAirComposition.checkbox.mass")); //$NON-NLS-1$
	    mass_btn.setToolTipText(Msg.getString("TabPanelAirComposition.checkbox.mass.tooltip")); //$NON-NLS-1$
	    //moles_btn = new JRadioButton(Msg.getString("TabPanelAirComposition.checkbox.moles")); //$NON-NLS-1$
	    //moles_btn.setToolTipText(Msg.getString("TabPanelAirComposition.checkbox.moles.tooltip")); //$NON-NLS-1$
	    //temperature_btn = new JRadioButton(Msg.getString("TabPanelAirComposition.checkbox.temperature")); //$NON-NLS-1$
	    //temperature_btn.setToolTipText(Msg.getString("TabPanelAirComposition.checkbox.temperature.tooltip")); //$NON-NLS-1$
	    
	    percent_btn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				tableModel.update();
			}
		});
	    pressure_btn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				tableModel.update();
			}
		});
	    mass_btn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				tableModel.update();
			}
		});
/*
	    moles_btn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				tableModel.update();
			}
		});
	    temperature_btn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				tableModel.update();
			}
		});
*/	    
	    percent_btn.setSelected(true);
		radioPane.add(percent_btn);
		radioPane.add(pressure_btn);
		radioPane.add(mass_btn);
		//radioPane.add(moles_btn);
		//radioPane.add(temperature_btn);
		
	    bG = new ButtonGroup();
	    bG.add(percent_btn);
	    bG.add(pressure_btn);
	    bG.add(mass_btn);		
	   // bG.add(moles_btn);
	    //bG.add(temperature_btn);
	    
		// Create scroll panel for the outer table panel.
		scrollPane = new JScrollPane();
		// scrollPane.setPreferredSize(new Dimension(257, 230));
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		centerContentPanel.add(scrollPane,BorderLayout.CENTER);

		tableModel = new TableModel(settlement);
		table = new ZebraJTable(tableModel);
	    SwingUtilities.invokeLater(() -> ColumnResizer.adjustColumnPreferredWidths(table));

		table.setCellSelectionEnabled(false);
		table.setDefaultRenderer(Double.class, new NumberCellRenderer());
		table.getColumnModel().getColumn(0).setPreferredWidth(55);
		table.getColumnModel().getColumn(1).setPreferredWidth(35);
		table.getColumnModel().getColumn(2).setPreferredWidth(25);
		table.getColumnModel().getColumn(3).setPreferredWidth(20);
		table.getColumnModel().getColumn(4).setPreferredWidth(20);
		table.getColumnModel().getColumn(5).setPreferredWidth(20);
		table.getColumnModel().getColumn(6).setPreferredWidth(20);

		table.setPreferredScrollableViewportSize(new Dimension(225, -1));
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

		table.setAutoCreateRowSorter(true);
		//if (!MainScene.OS.equals("linux")) {
		//	table.getTableHeader().setDefaultRenderer(new MultisortTableHeaderCellRenderer());
		//}

		TableStyle.setTableStyle(table);

		scrollPane.setViewportView(table);

	}

	/**
	 * Sets .
	 * @param value true or false.

	private void setMetric(boolean value) {
		if (value)
			buildings = manager.getSortedBuildings();
		else
			buildings = manager.getBuildingsWithThermal();
		tableModel.update();
	}
	 */
	
	public double getOverallComposition(int gas) {
		double result = 0;
		//List<Building> buildings = manager.getBuildingsWithLifeSupport();
		int size = buildingsCache.size();
		Iterator<Building> k = buildingsCache.iterator();
		while (k.hasNext()) {
			Building b = k.next();
			int id = b.getInhabitableID();
			double [][] vol = air.getPercentComposition();
			//System.out.println("vol.length : " + vol.length + "  vol[].length : " + vol[0].length);
			double percent = vol[gas][id];
			result += percent;
		}
		return result/size;
	}

	public String getSubtotal(int row) {
		double v = 0;
		
		if (percent_btn.isSelected()) {
			for (int gas = 0; gas < CompositionOfAir.numGases; gas++) {
				v += air.getPartialPressure()[gas][row];
			}			
			return String.format("%1.3f", v/air.getTotalPressure()[row] *100D);
		}
		else if (pressure_btn.isSelected()) {
			v = air.getTotalPressure()[row];
			// convert from atm to kPascal
			return String.format("%1.2f", v * CompositionOfAir.kPASCAL_PER_ATM);
		}
		//else if (moles_btn.isSelected()) {
		//	v = air.getTotalMoles()[row];
		//	return (String.format("%1.1e", v)).replaceAll("e+", "e"); 
		//}
		else if (mass_btn.isSelected()) {
			v = air.getTotalMass()[row];
			return String.format("%1.2f", v); 
		}
		//else if (temperature_btn.isSelected()) {
			/*
			for (int gas = 0; gas < CompositionOfAir.numGases; gas++) {
				v += air.getTemperature()[gas][row];
			}			
			return String.format("%2.1f", v/CompositionOfAir.numGases - CompositionOfAir.C_TO_K);
			*/
		//	return String.format("%2.1f", manager.getBuilding(row).getCurrentTemperature());
		//}
		else
			return null;

	}

	
	/**
	 * Updates the info on this panel.
	 */
	public void update() {

		List<Building> buildings = manager.getBuildingsWithLifeSupport();//getBuildings(BuildingFunction.LIFE_SUPPORT);
		int numBuildings = buildings.size();

		if (numBuildings != numBuildingsCache) {
			numBuildingsCache = numBuildings;
			buildingsCache = buildings;
		}
		else {

			double o2 = getOverallComposition(3);
			if (o2Cache != o2) {
				o2Cache = o2;
				o2Label.setText(
					Msg.getString("TabPanelAirComposition.label.o2", //$NON-NLS-1$
					fmt2.format(o2Cache)
					));
			}

			double cO2 = getOverallComposition(0);
			if (cO2Cache != cO2) {
				cO2Cache = cO2;
				cO2Label.setText(
					Msg.getString("TabPanelAirComposition.label.cO2", //$NON-NLS-1$
					fmt3.format(cO2Cache)
					));
			}

			double h2O = getOverallComposition(1);
			if (h2OCache != h2O) {
				h2OCache = h2O;
				h2OLabel.setText(
					Msg.getString("TabPanelAirComposition.label.h2O",  //$NON-NLS-1$
					fmt2.format(h2O)
					));
			}

			double n2 =  getOverallComposition(2);
			if (n2Cache != n2) {
				n2Cache = n2;
				n2Label.setText(
					Msg.getString("TabPanelAirComposition.label.n2",  //$NON-NLS-1$
					fmt1.format(n2)
					));
			}

			double ar = getOverallComposition(4);
			if (arCache != ar) {
				arCache = ar;
				arLabel.setText(
					Msg.getString("TabPanelAirComposition.label.ar",  //$NON-NLS-1$
					fmt2.format(ar)
					));
			}


			double totalPressure = Math.round(settlement.getAirPressure()/1000D*1000.0)/1000.0; // convert to kPascal by multiplying 1000
			if (totalPressureCache != totalPressure) {
				totalPressureCache = totalPressure;
				totalPressureLabel.setText(
					Msg.getString("TabPanelAirComposition.label.totalPressure",  //$NON-NLS-1$
					fmt2.format(totalPressureCache)//Math.round(totalPressureCache*10D)/10D
					));
			}

		}

		tableModel.update();
		TableStyle.setTableStyle(table);

	}

	/**
	 * Internal class used as model for the table.
	 */
	private class TableModel extends AbstractTableModel {

		/** default serial id. */
		private static final long serialVersionUID = 1L;

		private int size;

		private Settlement settlement;
		private BuildingManager manager;

		private List<Building> buildings = new ArrayList<>();;

		private CompositionOfAir air;

		private DecimalFormat fmt3 = new DecimalFormat(Msg.getString("decimalFormat3")); //$NON-NLS-1$
		private DecimalFormat fmt2 = new DecimalFormat(Msg.getString("decimalFormat2")); //$NON-NLS-1$
		private DecimalFormat fmt1 = new DecimalFormat(Msg.getString("decimalFormat1")); //$NON-NLS-1$

		private TableModel(Settlement settlement) {
			this.settlement = settlement;
			this.manager = settlement.getBuildingManager();
			this.air = settlement.getCompositionOfAir();
			this.buildings = selectBuildingsWithLS();
			this.size = buildings.size();

		}

		public List<Building> selectBuildingsWithLS() {
			return manager.getBuildingsWithLifeSupport();
		}

		public int getRowCount() {
			return size;
		}

		public int getColumnCount() {
			return 7;

		}

		public Class<?> getColumnClass(int columnIndex) {
			Class<?> dataType = super.getColumnClass(columnIndex);
			if (columnIndex == 0) dataType = String.class;//ImageIcon.class;
			else if (columnIndex == 1) dataType = Double.class;
			else if (columnIndex == 2) dataType = Double.class;
			else if (columnIndex == 3) dataType = Double.class;
			else if (columnIndex == 4) dataType = Double.class;
			else if (columnIndex == 5) dataType = Double.class;
			else if (columnIndex == 6) dataType = Double.class;
			return dataType;
		}

		public String getColumnName(int columnIndex) {
			if (columnIndex == 0) return Msg.getString("TabPanelAirComposition.column.buildingName"); //$NON-NLS-1$
			else if (columnIndex == 1) return Msg.getString("TabPanelAirComposition.column.total"); //$NON-NLS-1$
			else if (columnIndex == 2) return Msg.getString("TabPanelAirComposition.column.cO2"); //$NON-NLS-1$
			else if (columnIndex == 3) return Msg.getString("TabPanelAirComposition.column.ar"); //$NON-NLS-1$
			else if (columnIndex == 4) return Msg.getString("TabPanelAirComposition.column.n2"); //$NON-NLS-1$
			else if (columnIndex == 5) return Msg.getString("TabPanelAirComposition.column.o2"); //$NON-NLS-1$
			else if (columnIndex == 6) return Msg.getString("TabPanelAirComposition.column.h2o"); //$NON-NLS-1$

			else return null;
		}

		public Object getValueAt(int row, int column) {

			//Building b = buildings.get(row);
			Building b = manager.getInhabitableBuilding(row);

			// CO2, H2O, N2, O2, Others (Ar2, He, CH4...)
			if (column == 0) {
				return b.getNickName();
			}
			else if (column == 1) {
				//return air.getTotalPressure()[row]* CompositionOfAir.kPASCAL_PER_ATM;
				return getSubtotal(row);//getTotalPressure(row);
			}
			else if (column > 1) {
				//double amt = air.getPercentComposition()[column - 2][b.getInhabitableID()];//getComposition(column - 2);
				double amt = getValue(column - 2, b.getInhabitableID());
				if (amt == 0)
					return "N/A";
				else if (percent_btn.isSelected())
					return String.format("%1.3f", amt); 
				else if (pressure_btn.isSelected())
					return String.format("%1.2f", amt); 
				//else if (moles_btn.isSelected())
				//	return String.format("%1.1e", amt).replaceAll("e+0", "e"); 
				else if (mass_btn.isSelected())
					return String.format("%1.2f", amt);//.replaceAll("e+0", "e"); 
				//else if (temperature_btn.isSelected())
				//	return String.format("%3.1f", amt); 
				else if (column == 2 || column == 6)
					return fmt3.format(amt);
				else if (column == 3 || column == 4 || column == 5)
					return fmt2.format(amt);
				else
					return null;
			}
			else  {
				return null;
			}
		}

		public double getValue(int gas, int id) {
			//double[][] value = new double[CompositionOfAir.numGases][size];
			if (percent_btn.isSelected())
				return air.getPercentComposition()[gas][id];
			else if (pressure_btn.isSelected())
				return air.getPartialPressure()[gas][id] * CompositionOfAir.kPASCAL_PER_ATM;
			//else if (temperature_btn.isSelected())
			//	return air.getTemperature()[gas][id] - CompositionOfAir.C_TO_K;
			//else if (moles_btn.isSelected())
			//	return air.getNumMoles()[gas][id];
			else if (mass_btn.isSelected())
				return air.getMass()[gas][id];
			else
				return 0;
		}
		
/*		
		public double getMole(int gas) {
			double mole = 0;
			Iterator<Building> k = buildings.iterator();
			while (k.hasNext()) {
				Building b = k.next();
				int id = b.getInhabitableID();
				double [][] numMoles = air.getNumMoles();

				if (id < numMoles[0].length)
					mole = numMoles[gas][id];
				else
					mole = 0;
			}
			return mole;
		}	
		
		public double getMass(int gas) {
			double kg = 0;
			double moles = getMole(gas);
			if (gas == 0)
				kg = CompositionOfAir.CO2_MOLAR_MASS * moles;
			else if (gas == 1)
				kg = CompositionOfAir.ARGON_MOLAR_MASS * moles;
			else if (gas == 2)
				kg = CompositionOfAir.N2_MOLAR_MASS * moles;
			else if (gas == 3)
				kg = CompositionOfAir.O2_MOLAR_MASS * moles;
			else if (gas == 4)
				kg = 0;
							
			return kg;
		}
*/		
/*		
		public double getComposition(int gas) {
			double percent = 0;
			Iterator<Building> k = buildings.iterator();
			while (k.hasNext()) {
				Building b = k.next();
				int id = b.getInhabitableID();
				double [][] comp = air.getPercentComposition();

				if (id < comp[0].length)
					percent = comp[gas][id];
				else
					percent = 0;

			}
			return percent;
		}
*/
/*
		public double getTotalPressure(int row) {
			double [] tp = air.getTotalPressure();
			double p = 0;
			if (row < tp.length)
				p = tp[row];
			else
				p = 0;
			// convert from atm to kPascal
			return p * CompositionOfAir.kPASCAL_PER_ATM;
		}
*/
		public void update() {
			//int newSize = buildings.size();
			//if (size != newSize) {
			//	size = newSize;
			//	buildings = selectBuildingsWithLS();
				//Collections.sort(buildings);
			//}
			//else {
				List<Building> newBuildings = selectBuildingsWithLS();
				if (!buildings.equals(newBuildings)) {
					buildings = newBuildings;
					scrollPane.validate();
					//Collections.sort(buildings);
				}
			//}

			fireTableDataChanged();
		}
	}
}