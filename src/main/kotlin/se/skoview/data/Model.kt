package se.skoview.data

import pl.treksoft.kvision.redux.RAction
import pl.treksoft.kvision.redux.createReduxStore
import kotlin.js.Date
import kotlin.js.Math
import kotlin.js.Math.floor

//@Serializable
data class HippoState(
    // Status infomration
    val downloadingBaseItems: Boolean,
    val downloadingIntegrations: Boolean,
    val errorMessage: String?,

    // Base Items
    val integrationDates: List<String>,
    val statisticsDates: List<String>,
    val serviceComponents: Map<Int, ServiceComponent>,
    val logicalAddresses: Map<Int, LogicalAddress>,
    val serviceContracts: Map<Int, ServiceContract>,
    val serviceDomains: Map<Int, ServiceDomain>,
    val plattforms: Map<Int, Plattform>,
    val plattformChains: Map<Int, PlattformChain>,

    // Filter parameters
    val dateEffective: String,
    val dateEnd: String,

    val selectedConsumers: List<Int>,
    val selectedProducers: List<Int>,
    val selectedLogicalAddresses: List<Int>,
    val selectedContracts: List<Int>,
    val selectedDomains: List<Int>,
    val selectedPlattformChains: List<Int>,

    // Integrations data
    val integrationArrs: List<Integration>,
    val maxCounters: MaxCounter,
    val updateDates: List<String>,

    // View data structures
    val vServiceConsumers: List<ServiceComponent>,
    val vServiceProducers: List<ServiceComponent>,
    val vServiceDomains: List<ServiceDomain>,
    val vServiceContracts: List<ServiceContract>,
    val vDomainsAndContracts: List<BaseItem>,
    val vPlattformChains: List<PlattformChain>,
    val vLogicalAddresses: List<LogicalAddress>
)

// The extension function create the part of the URL to fetch integrations
fun HippoState.getParams(): String {

    //var params = "?dummy&contractId=379"
    var params = "?dummy"

    params += if (this.selectedConsumers.isNotEmpty()) this.selectedConsumers.joinToString(prefix = "&consumerId=", separator = ",") else ""
    params += if (this.selectedDomains.isNotEmpty()) this.selectedDomains.joinToString(prefix = "&domainId=", separator = ",") else ""
    params += if (this.selectedContracts.isNotEmpty()) this.selectedContracts.joinToString(prefix = "&contractId=", separator = ",") else ""
    params += if (this.selectedLogicalAddresses.isNotEmpty()) this.selectedLogicalAddresses.joinToString(prefix = "&logicalAddressId=", separator = ",") else ""
    params += if (this.selectedProducers.isNotEmpty()) this.selectedProducers.joinToString(prefix = "&producerId=", separator = ",") else ""

    params += "&dateEffective=" + this.dateEffective
    params += "&dateEnd=" + this.dateEnd

    // Separate plattforms now stored in filter, not the chain
    for (pcId in this.selectedPlattformChains) {
        val firstId = PlattformChain.map[pcId]?.first
        val lastId = PlattformChain.map[pcId]?.last
        params += "&firstPlattformId=" + firstId
        params += "&lastPlattformId=" + lastId
    }

    return params
}

fun HippoState.getBookmark(excludeDates: Boolean = false): String {
    var bookmark = ""

    bookmark += if (this.selectedConsumers.isNotEmpty()) this.selectedConsumers.joinToString(prefix = "c", separator = "c") else ""
    bookmark += if (this.selectedDomains.isNotEmpty()) this.selectedDomains.joinToString(prefix = "d", separator = "d") else ""
    bookmark += if (this.selectedContracts.isNotEmpty()) this.selectedContracts.joinToString(prefix = "C", separator = "C") else ""
    bookmark += if (this.selectedLogicalAddresses.isNotEmpty()) this.selectedLogicalAddresses.joinToString(prefix = "l", separator = "l") else ""
    bookmark += if (this.selectedProducers.isNotEmpty()) this.selectedProducers.joinToString(prefix = "p", separator = "p") else ""

    if (! excludeDates) {
        bookmark += "S" + date2DaysSinceEpoch(this.dateEffective)
        bookmark += "E" + date2DaysSinceEpoch(this.dateEnd)
    }

    // Separate plattforms now stored in filter, not the chain
    for (pcId in this.selectedPlattformChains) {
        val firstId = PlattformChain.map[pcId]?.first
        val lastId = PlattformChain.map[pcId]?.last
        bookmark += "F" + firstId
        bookmark += "L" + lastId
    }

    println("Bookmark is: $bookmark")
    return bookmark


}

fun date2DaysSinceEpoch(dateString: String): Double {
    val day = Date(dateString)

    return (day.getTime() / 8.64e7) - 16874  // Dived by number of millisecs since epoch (1/1 1970)
}
/*
    fun daysSinceEpoch2date(daysSinceEpoch) {
        var date = new Date((daysSinceEpoch + 16874) * 8.64e7);
        return date.toISOString().substring(0, 10);
    }
*/

fun HippoState.isItemFiltered(itemType: ItemType, id: Int): Boolean {
    return when (itemType) {
        ItemType.CONSUMER -> this.selectedConsumers.contains(id)
        ItemType.DOMAIN -> this.selectedDomains.contains(id)
        ItemType.CONTRACT -> this.selectedContracts.contains(id)
        ItemType.PLATTFORM_CHAIN -> this.selectedPlattformChains.contains(id)
        ItemType.LOGICAL_ADDRESS -> this.selectedLogicalAddresses.contains(id)
        ItemType.PRODUCER -> this.selectedProducers.contains(id)
        else -> {
            println("*** ERROR, unexpected type in isItemFiltered: $itemType")
            false
        }
    }
}

val INITIAL_STATE = HippoState(
    false,
    false,
    null,
    listOf(),
    listOf(),
    mapOf(),
    mapOf(),
    mapOf(),
    mapOf(),
    mapOf(),
    mapOf(),
    "", // todo: Verify if this is a good default - really want empty value
    "",
    listOf(),
    listOf(),
    listOf(),
    listOf(),
    listOf(),
    listOf(),
    listOf(),
    MaxCounter(0, 0, 0, 0, 0, 0),
    listOf(),
    listOf(),
    listOf(),
    listOf(),
    listOf(),
    listOf(),
    listOf(),
    listOf()
)

val store = createReduxStore(
    ::hippoReducer,
    INITIAL_STATE
)

