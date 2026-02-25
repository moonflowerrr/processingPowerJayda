/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2013-15 The Processing Foundation
  Copyright (c) 2010-13 Ben Fry and Casey Reas

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.mode.java;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JLabel;

import processing.app.Language;
import processing.app.ui.Editor;
import processing.app.ui.EditorButton;
import processing.app.ui.EditorToolbar;
import processing.mode.java.debug.Debugger;

import java.awt.Color;
import javax.swing.JColorChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.GridLayout;


public class JavaToolbar extends EditorToolbar {
  JavaEditor jeditor;

  EditorButton stepButton;
  EditorButton continueButton;


  public JavaToolbar(Editor editor) {
    super(editor);
    jeditor = (JavaEditor) editor;
  }


  /**
   * Check if 'debugger' is available and enabled.
   */
  private boolean isDebuggerArmed() {
    // 'jeditor' not ready yet because this is called by super()
    // 'debugger' also null during init
    if (jeditor == null) {
      return false;
    }
    return jeditor.isDebuggerEnabled();
  }

  public void showThemePicker() {
    // 1. Acceptance Criteria: Shown three options
    String[] options = { "Outer Theme", "Inner Coding Area", "Console" };
    String selection = (String) JOptionPane.showInputDialog(null, "Select element to change:",
        "Theme Customizer", JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

    if (selection == null) return;

    // 2. Acceptance Criteria: Color picker square with scrolls & Hex input
    // JColorChooser handles the hex box and preview box automatically.
    Color pickedColor = JColorChooser.showDialog(jeditor, "Customize " + selection, Color.WHITE);

    if (pickedColor != null) {
      String hex = String.format("#%02x%02x%02x", pickedColor.getRed(), pickedColor.getGreen(), pickedColor.getBlue());
      
      // 3. Map selection to Processing's internal Preference keys
      if (selection.equals("Outer Theme")) {
        processing.app.Preferences.set("header.color", hex); 
      } else if (selection.equals("Inner Coding Area")) {
        processing.app.Preferences.set("editor.bgcolor", hex);
        processing.app.Preferences.set("editor.fgcolor", "#" + Integer.toHexString(getContrastColor(pickedColor).getRGB()).substring(2));
      } else {
        processing.app.Preferences.set("console.color", hex);
      }

      // 4. Acceptance Criteria: Immediately display and save
      jeditor.getBase().handlePrefs(); // Applies and saves to preferences.txt
      jeditor.rebuildHeader(); 
    }
  }

  private void applyCustomColor(int optionIndex, Color pickedColor) {
    String hex = String.format("#%02x%02x%02x", pickedColor.getRed(), pickedColor.getGreen(), pickedColor.getBlue());

    if (optionIndex == 0) { // Outer Theme
      // Global force - This is what makes the "strips" turn pink
      javax.swing.UIManager.put("Panel.background", pickedColor);
      javax.swing.UIManager.put("ToolBar.background", pickedColor);
      javax.swing.UIManager.put("StatusBar.background", pickedColor);
      javax.swing.UIManager.put("Component.borderColor", pickedColor);
      
      processing.app.Preferences.set("header.color", hex);
      
      java.awt.Window window = javax.swing.SwingUtilities.getWindowAncestor(this);
      if (window != null) {
          vanquishBlueSurgically(window, pickedColor);
      }
    } 
    else if (optionIndex == 1) { // Inner Coding Area
      jeditor.getTextArea().getPainter().setBackground(pickedColor);
      processing.app.Preferences.set("editor.bgcolor", hex);
    }
    else if (optionIndex == 2) { // Console
      jeditor.getConsole().setBackground(pickedColor);
      processing.app.Preferences.set("console.color", hex);
    }

    processing.app.Preferences.save();
    javax.swing.SwingUtilities.updateComponentTreeUI(javax.swing.SwingUtilities.getRoot(this));
  }

  private void vanquishBlueSurgically(java.awt.Component comp, Color c) {
    String className = comp.getClass().getName();
    
    // THE SHIELD (Add Table and TextArea to keep them safe)
    if (comp == jeditor.getTextArea() || comp == jeditor.getConsole() || 
        className.contains("ErrorTable") || className.contains("TextArea")) {
        return;
    }

    // THE FORCEFUL PAINT
    // We target everything that isn't protected to ensure no blue remains
    comp.setBackground(c);
    
    if (comp instanceof javax.swing.JComponent) {
        javax.swing.JComponent jc = (javax.swing.JComponent) comp;
        jc.setOpaque(true); // Forces the pink to show through
        jc.setBorder(null); // Removes blue divider lines
        
        // This is the "Lock" that prevents FlatLaf from reverting to blue
        String hex = String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
        jc.putClientProperty("FlatLaf.style", "background: " + hex);
    }

    // RECURSE
    if (comp instanceof java.awt.Container) {
        for (java.awt.Component child : ((java.awt.Container)comp).getComponents()) {
            vanquishBlueSurgically(child, c);
        }
    }
  }

  private Color getContrastColor(Color color) {
    // Standard YIQ formula to determine if background is dark or light
    double yiq = ((color.getRed() * 299) + (color.getGreen() * 587) + (color.getBlue() * 114)) / 1000;
    return (yiq >= 128) ? Color.BLACK : Color.WHITE;
  }


  @Override
  public List<EditorButton> createButtons() {
    final boolean debug = isDebuggerArmed();

    List<EditorButton> outgoing = new ArrayList<>();

    final String runText = debug ?
      Language.text("toolbar.debug") : Language.text("toolbar.run");
    runButton = new EditorButton(this,
                                 "/lib/toolbar/run",
                                 runText,
                                 Language.text("toolbar.present")) {
      @Override
      public void actionPerformed(ActionEvent e) {
        handleRun(e.getModifiers());
      }
    };
    outgoing.add(runButton);

    if (debug) {
      stepButton = new EditorButton(this,
                                    "/lib/toolbar/step",
                                    Language.text("menu.debug.step"),
                                    Language.text("menu.debug.step_into"),
                                    Language.text("menu.debug.step_out")) {
        @Override
        public void actionPerformed(ActionEvent e) {
          Debugger d = jeditor.getDebugger();

          final int modifiers = e.getModifiers() &
            (ActionEvent.SHIFT_MASK | ActionEvent.ALT_MASK);
          if (modifiers == 0) {
            d.stepOver();
          } else if ((modifiers & ActionEvent.SHIFT_MASK) != 0) {
            d.stepInto();
          } else if ((modifiers & ActionEvent.ALT_MASK) != 0) {
            d.stepOut();
          }
        }
      };
      outgoing.add(stepButton);

      continueButton = new EditorButton(this,
                                        "/lib/toolbar/continue",
                                        Language.text("menu.debug.continue")) {
        @Override
        public void actionPerformed(ActionEvent e) {
          //jeditor.handleContinue();
          jeditor.getDebugger().continueDebug();
        }
      };
      outgoing.add(continueButton);
    }

    stopButton = new EditorButton(this,
                                  "/lib/toolbar/stop",
                                  Language.text("toolbar.stop")) {
      @Override
      public void actionPerformed(ActionEvent e) {
        handleStop();
      }
    };
    outgoing.add(stopButton);

    // --- ADD THE THEME BUTTON HERE ---
    EditorButton themeButton = new EditorButton(this,
                                  "/lib/toolbar/run", 
                                  "Toggle Theme") {
      @Override
      public void actionPerformed(ActionEvent e) {
        String[] options = {"Outer Theme", "Inner Coding Area", "Console"};
        int selection = JOptionPane.showOptionDialog(jeditor, 
            "Which element would you like to change?", 
            "Theme Customizer", 
            JOptionPane.DEFAULT_OPTION, 
            JOptionPane.PLAIN_MESSAGE, 
            null, options, options[0]);

        if (selection != -1) {
          // JColorChooser fulfills criteria for: Hex box, Preview box, and Sliders
          Color pickedColor = JColorChooser.showDialog(jeditor, "Pick your color", Color.WHITE);
          if (pickedColor != null) {
            applyCustomColor(selection, pickedColor);
          }
        }
      }
    };
    outgoing.add(themeButton);

    return outgoing;
  }


  @Override
  public void addModeButtons(Box box, JLabel label) {
    EditorButton debugButton =
      new EditorButton(this, "/lib/toolbar/debug",
                       Language.text("toolbar.debug")) {
      @Override
      public void actionPerformed(ActionEvent e) {
        jeditor.toggleDebug();
      }
    };

    if (isDebuggerArmed()) {
      debugButton.setSelected(true);
    }
    box.add(debugButton);
    addGap(box);
  }


  @Override
  public void handleRun(int modifiers) {
    boolean shift = (modifiers & ActionEvent.SHIFT_MASK) != 0;
    if (shift) {
      jeditor.handlePresent();
    } else {
      jeditor.handleRun();
    }
  }


  @Override
  public void handleStop() {
    jeditor.handleStop();
  }


  public void activateContinue() {
    continueButton.setSelected(true);
    repaint();
  }


  protected void deactivateContinue() {
    continueButton.setSelected(false);
    repaint();
  }


  protected void activateStep() {
    stepButton.setSelected(true);
    repaint();
  }


  protected void deactivateStep() {
    stepButton.setSelected(false);
    repaint();
  }
}
