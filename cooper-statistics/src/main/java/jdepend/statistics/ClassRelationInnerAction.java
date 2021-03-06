package jdepend.statistics;

import java.awt.event.ActionEvent;

import jdepend.core.local.score.ScoreInfo;
import jdepend.framework.exception.JDependException;
import jdepend.framework.log.LogUtil;
import jdepend.framework.ui.graph.GraphData;
import jdepend.framework.ui.graph.GraphDataItem;
import jdepend.framework.ui.graph.GraphUtil;
import jdepend.model.result.AnalysisResult;
import jdepend.model.util.JavaClassRelationUtil;

public class ClassRelationInnerAction extends ScoreListAction {

	public ClassRelationInnerAction(StaticsFrame frame) {
		super(frame, "类关系内外比例分析");
	}

	@Override
	protected void analyse(ActionEvent e) throws JDependException {
		GraphData graph = new GraphData();
		GraphDataItem item;
		String title;

		JavaClassRelationUtil javaClassRelationUtil = null;
		AnalysisResult result;
		String group;
		String command;
		for (ScoreInfo scoreInfo : scoreCollection.getScoreInfos()) {
			result = scoreCollection.getTheResult(scoreInfo);
			javaClassRelationUtil = new JavaClassRelationUtil(result);
			item = new GraphDataItem();
			group = result.getRunningContext().getGroup();
			command = result.getRunningContext().getCommand();
			title = group + " " + command + " 类关系内外比例";
			item.setTitle(title);
			item.setType(GraphDataItem.PIE);

			item.setDatas(javaClassRelationUtil.getInners());
			graph.addItem(item);

			this.progress();
			LogUtil.getInstance(ClassRelationInnerAction.class).systemLog(
					"分析了[" + group + "][" + command + "]的ClassRelationInner");
		}

		this.addResult("类关系内外比例饼图", GraphUtil.getInstance().createGraph(graph));

	}
}
