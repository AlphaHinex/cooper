package jdepend.report.ui;

import java.awt.BorderLayout;
import java.util.Collection;

import jdepend.framework.ui.CooperDialog;
import jdepend.framework.ui.JDependFrame;
import jdepend.model.JavaClass;

public class ClassListDialog extends CooperDialog {

	public ClassListDialog(JDependFrame frame, Collection<JavaClass> javaClasses) {
		super("类列表");
		getContentPane().setLayout(new BorderLayout());

		ClassListPanel classListPanel = new ClassListPanel(frame);
		this.add(classListPanel);
		classListPanel.showClassList(javaClasses);

		classListPanel.initPopupMenu(new JavaClassMoveToDialogListener() {
			@Override
			public void onFinish() {
				ClassListDialog.this.dispose();
			}
		});
	}

	public ClassListDialog(JDependFrame frame, jdepend.model.Component component) {
		super("类列表");
		getContentPane().setLayout(new BorderLayout());

		ClassListPanel classListPanel = new ClassListPanel(frame);
		this.add(classListPanel);
		classListPanel.showClassList(component);

		classListPanel.initPopupMenu(new JavaClassMoveToDialogListener() {
			@Override
			public void onFinish() {
				ClassListDialog.this.dispose();
			}
		});
	}

	public ClassListDialog(JDependFrame frame) {
		super("类列表");
		this.setSize(frame.getScrSize().width, frame.getScrSize().height);
		this.setLocationRelativeTo(null);// 窗口在屏幕中间显示
		getContentPane().setLayout(new BorderLayout());

		ClassListPanel classListPanel = new ClassListPanel(frame);

		classListPanel.initPopupMenu(new JavaClassMoveToDialogListener() {
			@Override
			public void onFinish() {
				ClassListDialog.this.dispose();
			}
		});

		ClassListOperationPanel classListOperationPanel = new ClassListOperationPanel(classListPanel);

		this.add(classListOperationPanel);
	}

}
