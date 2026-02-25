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
    if (optionIndex == 0) { // Outer Theme (The Whole Top Border)
        this.setOpaque(true);
        this.setBackground(pickedColor);
        
        // Target the Header (the container for the toolbar)
        java.awt.Container header = this.getParent();
        if (header != null) {
            header.setBackground(pickedColor);
            if (header instanceof javax.swing.JComponent) {
                ((javax.swing.JComponent)header).setOpaque(true);
            }
            // Target the Editor's main container to fill the rest of the border
            java.awt.Container mainApp = header.getParent();
            if (mainApp != null) {
                mainApp.setBackground(pickedColor);
            }
            header.repaint();
        }
    } else if (optionIndex == 1) { // Inner Coding Area
        jeditor.getTextArea().getPainter().setBackground(pickedColor);
        Color textColor = getContrastColor(pickedColor);
        jeditor.getTextArea().getPainter().setForeground(textColor);
    } else if (optionIndex == 2) { // Console (The Bottom Area)
        jeditor.getConsole().setOpaque(true);
        jeditor.getConsole().setBackground(pickedColor);
        
        // Target the viewport to ensure the color fills the whole box
        java.awt.Container viewport = jeditor.getConsole().getParent();
        if (viewport != null && viewport instanceof javax.swing.JComponent) {
            javax.swing.JComponent jv = (javax.swing.JComponent)viewport;
            jv.setOpaque(true);
            jv.setBackground(pickedColor);
            jv.repaint(); // Fixed: Now inside the if-block scope
        }
    }

    // Acceptance Criteria: Text contrast update
    Color contrast = getContrastColor(pickedColor);
    if (optionIndex == 0) {
        for (java.awt.Component c : this.getComponents()) {
            c.setForeground(contrast);
        }
    }

    // Immediate redraw
    this.revalidate();
    this.repaint();
    jeditor.getTextArea().repaint();
    jeditor.getConsole().repaint();
  }

    // Acceptance Criteria: Text must change between white and black
    Color contrast = getContrastColor(pickedColor);
    if (optionIndex == 0) {
        // This updates the text labels in the toolbar to be readable
        for (java.awt.Component c : this.getComponents()) {
            c.setForeground(contrast);
        }
    }

    // Acceptance Criteria: The chosen color must immediately display
    this.revalidate();
    this.repaint();
    jeditor.getTextArea().repaint();
    jeditor.getConsole().repaint();
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
