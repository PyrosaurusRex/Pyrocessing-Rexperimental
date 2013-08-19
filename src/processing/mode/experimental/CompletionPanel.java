package processing.mode.experimental;
import static processing.mode.experimental.ExperimentalMode.log;
import static processing.mode.experimental.ExperimentalMode.logE;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.text.BadLocationException;

import processing.app.syntax.JEditTextArea;

public class CompletionPanel {
  private JList completionList;

  private JPopupMenu popupMenu;

  private String subWord;

  private int insertionPosition;

  private TextArea textarea;

  private JScrollPane scrollPane;
  
  protected DebugEditor editor;

  public CompletionPanel(JEditTextArea textarea, int position, String subWord,
                         DefaultListModel items, Point location, DebugEditor dedit) {
    this.textarea = (TextArea) textarea;
    editor = dedit;
    this.insertionPosition = position;
    if (subWord.indexOf('.') != -1)
      this.subWord = subWord.substring(subWord.lastIndexOf('.') + 1);
    else
      this.subWord = subWord;
    popupMenu = new JPopupMenu();
    popupMenu.removeAll();
    popupMenu.setOpaque(false);
    popupMenu.setBorder(null);
    scrollPane = new JScrollPane();
    scrollPane.setViewportView(completionList = createSuggestionList(position, items));
    popupMenu.add(scrollPane, BorderLayout.CENTER);
    popupMenu.setPopupSize(280, 250); //TODO: Eradicate this evil
    this.textarea.errorCheckerService.astGenerator
        .updateJavaDoc((CompletionCandidate) completionList.getSelectedValue());
    popupMenu.show(textarea, location.x, textarea.getBaseline(0, 0)
        + location.y);
    log("Suggestion constructed" + System.nanoTime());
  }

  public boolean isVisible() {
    return popupMenu.isVisible();
  }
  
  public void setVisible(boolean v){
    log("Pred popup visible.");
    popupMenu.setVisible(v);
  }

  private JList createSuggestionList(final int position,
                                    final DefaultListModel items) {

    JList list = new JList(items);
    list.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.setSelectedIndex(0);
    list.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          insertSelection();
          hideSuggestion();
        }
      }
    });
    list.setCellRenderer(new CustomListRenderer());
    return list;
  }
  
  public boolean updateList(final DefaultListModel items, String newSubword, int position){
    scrollPane.getViewport().removeAll();    
    Dimension dimen = popupMenu.getSize();
    completionList.setModel(items);
    completionList.validate();    
    completionList.setSelectedIndex(0);
    scrollPane.setViewportView(completionList);
    scrollPane.validate();
    popupMenu.setSize(dimen);
    
    this.subWord = new String(newSubword);
    if (subWord.indexOf('.') != -1)
      this.subWord = subWord.substring(subWord.lastIndexOf('.') + 1);
    insertionPosition = position;
    log("Suggestion updated" + System.nanoTime());
    return true;
  }

  public boolean insertSelection() {
    if (completionList.getSelectedValue() != null) {
      try {
        String selectedSuggestion = ((CompletionCandidate) completionList
            .getSelectedValue()).getCompletionString().substring(subWord.length());
        logE(subWord+" <= subword,Inserting suggestion=> " + selectedSuggestion);
        textarea.getDocument().remove(insertionPosition-subWord.length(), subWord.length());
        textarea.getDocument().insertString(insertionPosition-subWord.length(),
                                            ((CompletionCandidate) completionList
                                            .getSelectedValue()).getCompletionString(), null);
        if(selectedSuggestion.endsWith(")"))
        {
          if(!selectedSuggestion.endsWith("()")){
            int x = selectedSuggestion.indexOf('(');
            if(x != -1){
              //log("X................... " + x);
              textarea.setCaretPosition(insertionPosition + (x+1));
            }
          }
        }
        else {
          textarea.setCaretPosition(insertionPosition + selectedSuggestion.length());
        }
        return true;
      } catch (BadLocationException e1) {
        e1.printStackTrace();
      }
      hideSuggestion();
    }
    return false;
  }

  public void hideSuggestion() {
    popupMenu.setVisible(false);
    log("Suggestion hidden" + System.nanoTime());
    //textarea.errorCheckerService.astGenerator.jdocWindowVisible(false);
  }

  public void moveUp() {
    if (completionList.getSelectedIndex() == 0) {
      scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
      selectIndex(completionList.getModel().getSize() - 1);
      return;
    } else {
      int index = Math.max(completionList.getSelectedIndex() - 1, 0);
      selectIndex(index);
    }
    int step = scrollPane.getVerticalScrollBar().getMaximum()
        / completionList.getModel().getSize();
    scrollPane.getVerticalScrollBar().setValue(scrollPane
                                                   .getVerticalScrollBar()
                                                   .getValue()
                                                   - step);
    textarea.errorCheckerService.astGenerator
        .updateJavaDoc((CompletionCandidate) completionList.getSelectedValue());

  }

  public void moveDown() {
    if (completionList.getSelectedIndex() == completionList.getModel().getSize() - 1) {
      scrollPane.getVerticalScrollBar().setValue(0);
      selectIndex(0);
      return;
    } else {
      int index = Math.min(completionList.getSelectedIndex() + 1, completionList.getModel()
          .getSize() - 1);
      selectIndex(index);
    }
    textarea.errorCheckerService.astGenerator
        .updateJavaDoc((CompletionCandidate) completionList.getSelectedValue());
    int step = scrollPane.getVerticalScrollBar().getMaximum()
        / completionList.getModel().getSize();
    scrollPane.getVerticalScrollBar().setValue(scrollPane
                                                   .getVerticalScrollBar()
                                                   .getValue()
                                                   + step);
  }

  private void selectIndex(int index) {
    completionList.setSelectedIndex(index);
//      final int position = textarea.getCaretPosition();
//      SwingUtilities.invokeLater(new Runnable() {
//        @Override
//        public void run() {
//          textarea.setCaretPosition(position);
//        };
//      });
  }
  
  protected class CustomListRenderer extends
      javax.swing.DefaultListCellRenderer {
    //protected final ImageIcon classIcon, fieldIcon, methodIcon;    
   
    public Component getListCellRendererComponent(JList list, Object value,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {
      JLabel label = (JLabel) super.getListCellRendererComponent(list, value,
                                                                 index,
                                                                 isSelected,
                                                                 cellHasFocus);
      if (value instanceof CompletionCandidate) {
        CompletionCandidate cc = (CompletionCandidate) value;
        switch (cc.getType()) {
        case CompletionCandidate.LOCAL_FIELD:
        case CompletionCandidate.PREDEF_FIELD:
          label.setIcon(editor.dmode.fieldIcon);
          break;
        case CompletionCandidate.LOCAL_METHOD:
        case CompletionCandidate.PREDEF_METHOD:
          label.setIcon(editor.dmode.methodIcon);
          break;
        case CompletionCandidate.LOCAL_CLASS:
        case CompletionCandidate.PREDEF_CLASS:
          label.setIcon(editor.dmode.classIcon);
          break;

        default:
          break;
        }

      }
      return label;
    }
  }
  
}