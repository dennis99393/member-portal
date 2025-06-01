package org.dallasmakerspace.dataviz

import javax.inject.Inject
import org.dallasmakerspace.models.DataVizResponse

class DataVizRouter
@Inject
constructor(dataVizReportSet: Set<@JvmSuppressWildcards DataVizReport>) {
  private val reportMap: Map<String, DataVizReport> = dataVizReportSet.associateBy { it.getName() }

  suspend fun route(method: String, params: Map<String, List<String>>): DataVizResponse? =
      reportMap[method]?.getData(params)
}

/**
 * Abstract class representing a data visualization report. Each report has a unique name and must
 * implement the `getData` method to provide the data for the report.
 */
open class DataVizReport @Inject constructor() {

  open fun getName(): String = this::class.simpleName ?: "UnknownReport"

  /**
   * Abstract method to retrieve the data for the report. Implementations of this class must provide
   * the logic to generate or fetch the data for the report.
   *
   * @return A `DataVizResponse` object containing the report data.
   */
  open suspend fun getData(params: Map<String, List<String>>): DataVizResponse? = null
}
