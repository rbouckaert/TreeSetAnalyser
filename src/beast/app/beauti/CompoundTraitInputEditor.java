package beast.app.beauti;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.CellEditorListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import beast.app.beauti.BeautiDoc;
import beast.app.beauti.GuessPatternDialog;
import beast.app.draw.ListInputEditor;
import beast.app.draw.SmallLabel;
import beast.core.BEASTInterface;
import beast.core.Input;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.AlignmentFromTrait;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.datatype.UserDataType;
import beast.evolution.tree.TraitSet;
import beast.evolution.tree.TreeInterface;
import tsa.correlatedcharacters.polycharacter.CompoundTreeLikelihood;



public class CompoundTraitInputEditor extends ListInputEditor {
	private static final long serialVersionUID = 1L;

	public CompoundTraitInputEditor(BeautiDoc doc) {
		super(doc);
	}

	@Override
	public Class<?> baseType() {
		return CompoundTreeLikelihood.class;
	}

	CompoundTreeLikelihood likelihood;
	TreeInterface tree;
    TraitSet traitSet;
    JTextField traitEntry;
    List<String> sTaxa;
    Object[][] tableData;
    JTable table;
    UserDataType dataType;

    String m_sPattern = ".*_(..).*";

	@Override
	public void init(Input<?> input, BEASTInterface plugin, int itemNr,	ExpandOption bExpandOption, boolean bAddButtons) {
        m_bAddButtons = bAddButtons;
        m_input = input;
        m_beastObject = plugin;
        this.itemNr = itemNr;
        m_bAddButtons = bAddButtons;
		this.itemNr = itemNr;
		if (itemNr >= 0) {
			likelihood = (CompoundTreeLikelihood) ((ArrayList<?>)input.get()).get(itemNr);
		} else {
			likelihood = (CompoundTreeLikelihood) ((ArrayList<?>)input.get()).get(0);
		}
	}

	public void initPanel(CompoundTreeLikelihood likelihood_) {
		likelihood = likelihood_;
		m_beastObject = likelihood.dataInput.get();
		try {
			m_input = m_beastObject.getInput("traitSet");
		}catch (Exception e) {
			// TODO: handle exception
		}
		
        tree = likelihood.treeInput.get();
        if (tree != null) {
        	Alignment data = likelihood.dataInput.get();
        	if (!(data instanceof AlignmentFromTrait)) {
        		return;
        	}
    		AlignmentFromTrait traitData = (AlignmentFromTrait) data;
            m_input = traitData.traitInput;
            m_beastObject = traitData;
            traitSet = traitData.traitInput.get();
            
            if (traitSet == null) {
                traitSet = new TraitSet();
                String context = BeautiDoc.parsePartition(likelihood.getID());
                traitSet.setID("traitSet." + context);
                try {
                traitSet.initByName("traitname", "discrete",
                        "taxa", tree.getTaxonset(),
                        "value", "");
                m_input.setValue(traitSet, m_beastObject);
                data.initAndValidate();
                } catch (Exception e) {
					// TODO: handle exception
				}
            }
            
            
            dataType = (UserDataType)traitData.userDataTypeInput.get();

            Box box = Box.createVerticalBox();

            if (traitSet != null) {
                box.add(createButtonBox());
                box.add(createListBox());

                Box box2 = Box.createHorizontalBox();
                box2.add(createTypeList(0));
                box2.add(createTypeList(1));
                box.add(box2);
            }
            add(box);
            validateInput();
            // synchronise with table, useful when taxa have been deleted
            convertTableDataToDataType();
            convertTableDataToTrait();
        }
    } // init


	Set<String> [][] type = new Set[2][2];

    private Component createTypeList(final int index) {
    	String [] strs = dataType.codeMapInput.get().split(",");
    	type[0][0] = new HashSet<>();
    	type[0][1] = new HashSet<>();
    	type[1][0] = new HashSet<>();
    	type[1][1] = new HashSet<>();
    	
    	for (String str: strs) {
    		System.out.println(str);
    		String [] strs2 = str.split("=");
    		if (strs2[1].trim().equals("0")) {
    			String [] strs3 = strs2[0].split("-");
    			type[0][0].add(strs3[0]);
    			type[1][0].add(strs3[1]);
    		} else if (strs2[1].trim().equals("3")) {
    			String [] strs3 = strs2[0].split("-");
    			type[0][1].add(strs3[0]);
    			type[1][1].add(strs3[1]);
    		}
    	}
    	
    	
    	Set<String> all = new HashSet<>();
    	all.addAll(type[index][0]);
    	all.addAll(type[index][1]);
    	String [] all_ = all.toArray(new String[]{});
    	Arrays.sort(all_);

    	Box box = Box.createVerticalBox();
    	box.add(new JLabel("Categories for trait " + (index + 1) + " "));
    	for (String str : all_) {
    		final JCheckBox checkbox = new JCheckBox(str, type[index][1].contains(str));
    		checkbox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					toggle(index, (JCheckBox) e.getSource());
				}
			});
    		box.add(checkbox);
    	}
    	
		return box;
	}

	private void toggle(int index, JCheckBox value) {
		System.out.println("Toggle " + index + " " + value);
		String str = value.getText();
		if (value.isSelected()) {
			type[index][0].remove(str);
			type[index][1].add(str);
		} else {
			type[index][1].remove(str);
			type[index][0].add(str);
		}
		
		StringBuilder b = new StringBuilder();
		for(String s0 : type[0][0]) {
			for(String s1 : type[1][0]) {
				b.append(s0 + "-" + s1 + "=0,");
			}			
		}
		for(String s0 : type[0][1]) {
			for(String s1 : type[1][0]) {
				b.append(s0 + "-" + s1 + "=1,");
			}			
		}
		for(String s0 : type[0][0]) {
			for(String s1 : type[1][1]) {
				b.append(s0 + "-" + s1 + "=2,");
			}			
		}
		for(String s0 : type[0][1]) {
			for(String s1 : type[1][1]) {
				b.append(s0 + "-" + s1 + "=3,");
			}			
		}
		b.delete(b.length()-1, b.length());
		dataType.codeMapInput.setValue(b.toString(), dataType);
	}

	private Component createListBox() {
    	try {
    		traitSet.taxaInput.get().initAndValidate();
    		
        	TaxonSet taxa = tree.getTaxonset();
        	taxa.initAndValidate();
        	sTaxa = taxa.asStringList();
    	} catch (Exception e) {
			// TODO: handle exception
            sTaxa = traitSet.taxaInput.get().asStringList();
		}
        String[] columnData = new String[]{"Name", "Trait"};
        tableData = new Object[sTaxa.size()][2];
        convertTraitToTableData();
        // set up table.
        // special features: background shading of rows
        // custom editor allowing only Date column to be edited.
        table = new JTable(tableData, columnData) {
            private static final long serialVersionUID = 1L;

            // method that induces table row shading
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int Index_row, int Index_col) {
                Component comp = super.prepareRenderer(renderer, Index_row, Index_col);
                //even index, selected or not selected
                if (isCellSelected(Index_row, Index_col)) {
                    comp.setBackground(Color.lightGray);
                } else if (Index_row % 2 == 0 && !isCellSelected(Index_row, Index_col)) {
                    comp.setBackground(new Color(237, 243, 255));
                } else {
                    comp.setBackground(Color.white);
                }
                return comp;
            }
        };

        // set up editor that makes sure only doubles are accepted as entry
        // and only the Date column is editable.
        table.setDefaultEditor(Object.class, new TableCellEditor() {
            JTextField m_textField = new JTextField();
            int m_iRow
                    ,
                    m_iCol;

            @Override
            public boolean stopCellEditing() {
                table.removeEditor();
                String sText = m_textField.getText();
                if (sText == "") {
                	return false;
                }
                tableData[m_iRow][m_iCol] = sText;
                convertTableDataToTrait();
                convertTraitToTableData();
                validateInput();
                return true;
            }

            @Override
            public boolean isCellEditable(EventObject anEvent) {
                return table.getSelectedColumn() == 1;
            }


            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int iRow, int iCol) {
                if (!isSelected) {
                    return null;
                }
                m_iRow = iRow;
                m_iCol = iCol;
                m_textField.setText((String) value);
                return m_textField;
            }

            @Override
            public boolean shouldSelectCell(EventObject anEvent) {
                return false;
            }

            @Override
            public void removeCellEditorListener(CellEditorListener l) {
            }

            @Override
            public Object getCellEditorValue() {
                return null;
            }

            @Override
            public void cancelCellEditing() {
            }

            @Override
            public void addCellEditorListener(CellEditorListener l) {
            }

        });
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        table.setRowHeight(24);
        JScrollPane scrollPane = new JScrollPane(table);
        return scrollPane;
    } // createListBox

    /* synchronise table with data from traitSet Plugin */
    private void convertTraitToTableData() {
        for (int i = 0; i < tableData.length; i++) {
            tableData[i][0] = sTaxa.get(i);
            tableData[i][1] = "";
        }
        String trait = traitSet.traitsInput.get();
        if (trait.trim().length() == 0) {
        	return;
        }
        String[] sTraits = trait.split(",");
        for (String sTrait : sTraits) {
            sTrait = sTrait.replaceAll("\\s+", " ");
            String[] sStrs = sTrait.split("=");
            String value = null;
            if (sStrs.length != 2) {
            	value = "";
                //throw new Exception("could not parse trait: " + sTrait);
            } else {
            	value = sStrs[1].trim();
            }
            String sTaxonID = sStrs[0].trim();
            int iTaxon = sTaxa.indexOf(sTaxonID);
            if (iTaxon < 0) {
            	System.err.println(sTaxonID);
//                throw new Exception("Trait (" + sTaxonID + ") is not a known taxon. Spelling error perhaps?");
            } else {
	            tableData[iTaxon][0] = sTaxonID;
	            tableData[iTaxon][1] = value;
            }
        }

        if (table != null) {
            for (int i = 0; i < tableData.length; i++) {
                table.setValueAt(tableData[i][1], i, 1);
            }
        }
    } // convertTraitToTableData

    /**
     * synchronise traitSet Plugin with table data
     */
    private void convertTableDataToTrait() {
        String sTrait = "";
        //Set<String> values = new HashSet<String>(); 
        for (int i = 0; i < tableData.length; i++) {
            sTrait += sTaxa.get(i) + "=" + tableData[i][1];
            if (i < tableData.length - 1) {
                sTrait += ",\n";
            }
        }
        try {
            traitSet.traitsInput.setValue(sTrait, traitSet);
        } catch (Exception e) {
            e.printStackTrace();
        }
        convertTableDataToDataType();
    }

    private void convertTableDataToDataType() {
        List<String> values = new ArrayList<String>(); 
        for (int i = 0; i < tableData.length; i++) {
        	if (tableData[i][1].toString().trim().length() > 0 && !values.contains(tableData[i][1].toString())) {
        		values.add(tableData[i][1].toString());
        	}
        }
        validateInput();
    }

    /**
     * create box with comboboxes for selection units and trait name *
     */
    private Box createButtonBox() {
        Box buttonBox = Box.createHorizontalBox();

        JLabel label = new JLabel("Trait: ");
        //label.setMaximumSize(new Dimension(1024, 20));
        buttonBox.add(label);

        traitEntry = new JTextField(traitSet.traitNameInput.get());
        traitEntry.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void removeUpdate(DocumentEvent e) {update();}
			@Override
			public void insertUpdate(DocumentEvent e) {update();}
			@Override
			public void changedUpdate(DocumentEvent e) {update();}
			void update() {
				try {
					traitSet.traitNameInput.setValue(traitEntry.getText(), traitSet);
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
		});
        traitEntry.setColumns(12);
        buttonBox.add(traitEntry);
        buttonBox.add(Box.createHorizontalGlue());

        JButton guessButton = new JButton("Guess");
        guessButton.setName("guess");
        guessButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	guess();
            }
        });
        buttonBox.add(guessButton);


        m_validateLabel = new SmallLabel("x", Color.orange);
        m_validateLabel.setVisible(false);
        buttonBox.add(m_validateLabel);
        
        return buttonBox;
    } // createButtonBox
    
    
    private void guess() {
        GuessPatternDialog dlg = new GuessPatternDialog(this, m_sPattern);
        //dlg.setName("GuessPatternDialog");
        String sTrait = "";
        switch (dlg.showDialog("Guess traits from taxon names")) {
        case canceled : return;
        case trait: sTrait = dlg.getTrait();
        	break;
        case pattern:
            String sPattern = dlg.getPattern(); 
            try {
                Pattern pattern = Pattern.compile(sPattern);
                for (String sTaxon : sTaxa) {
                    Matcher matcher = pattern.matcher(sTaxon);
                    if (matcher.find()) {
                        String sMatch = matcher.group(1);
                        if (sTrait.length() > 0) {
                            sTrait += ",";
                        }
                        sTrait += sTaxon + "=" + sMatch;
                    }
                    m_sPattern = sPattern;
                }
            } catch (Exception e) {
                return;
            }
            break;
        }
        try {
        	traitSet.traitsInput.setValue(sTrait, traitSet);
        } catch (Exception e) {
			// TODO: handle exception
		}
        convertTraitToTableData();
        convertTableDataToTrait();
        convertTableDataToDataType();
        repaint();
    }
	
	@Override
	public void validateInput() {
		// check all values are specified
		if (tableData == null) {
			return;
		}
        for (int i = 0; i < tableData.length; i++) {
        	if (tableData[i][1].toString().trim().length() == 0) {
        		m_validateLabel.setVisible(true);
        		m_validateLabel.setToolTipText("trait for " + tableData[i][0] + " needs to be specified");
        		m_validateLabel.repaint();
        		return;
        	}
        }
		m_validateLabel.setVisible(false);
		super.validateInput();
	}
}
