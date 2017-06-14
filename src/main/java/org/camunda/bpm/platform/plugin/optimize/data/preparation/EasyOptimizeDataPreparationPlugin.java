package org.camunda.bpm.platform.plugin.optimize.data.preparation;

import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.history.producer.HistoryEventProducer;

public class EasyOptimizeDataPreparationPlugin extends AbstractProcessEnginePlugin {

  public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
    HistoryEventProducer historyEventProducer = processEngineConfiguration.getHistoryEventProducer();
    if (historyEventProducer == null) {
      historyEventProducer = new OptimizeAdaptionHistoryEventProducer();
      processEngineConfiguration.setHistoryEventProducer(historyEventProducer);
    }
  }

}
