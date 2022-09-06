package io.beatmaps.user.alerts

import external.Axios
import external.CancelTokenSource
import external.generateConfig
import io.beatmaps.api.UserAlert
import io.beatmaps.api.UserAlertStats
import io.beatmaps.common.Config
import io.beatmaps.common.api.EAlertType
import io.beatmaps.shared.InfiniteScroll
import io.beatmaps.shared.InfiniteScrollElementRenderer
import io.beatmaps.shared.buildURL
import io.beatmaps.shared.includeIfNotNull
import org.w3c.dom.HTMLDivElement
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.createRef
import react.dom.div
import react.dom.h1
import react.dom.h6
import react.dom.jsStyle
import react.router.dom.RouteResultHistory
import react.setState

external interface AlertsPageProps : RProps {
    var history: RouteResultHistory
    var read: Boolean?
    var filters: List<EAlertType>?
}

external interface AlertsPageState : RState {
    var alertStats: UserAlertStats?
    var resultsKey: Any?
    var hiddenAlerts: List<Int>?
}

class AlertsPage : RComponent<AlertsPageProps, AlertsPageState>() {
    private val resultsColumn = createRef<HTMLDivElement>()

    override fun componentWillMount() {
        Axios.get<UserAlertStats>(
            "${Config.apibase}/alerts/stats",
            generateConfig<String, UserAlertStats>()
        ).then {
            setState {
                alertStats = it.data
            }
        }
    }

    override fun componentWillReceiveProps(nextProps: AlertsPageProps) {
        setState {
            resultsKey = Any()
            hiddenAlerts = listOf()
        }
    }

    private val loadPage = { toLoad: Int, token: CancelTokenSource ->
        val type = if (props.read == true) "read" else "unread"
        val typeFilter = if (props.filters?.any() == true) "?type=${props.filters?.joinToString(",") { it.name.lowercase() }}" else ""
        Axios.get<List<UserAlert>>(
            "${Config.apibase}/alerts/$type/$toLoad$typeFilter",
            generateConfig<String, List<UserAlert>>(token.token)
        ).then {
            return@then it.data
        }
    }

    private fun toURL(read: Boolean? = props.read, filters: List<EAlertType>? = props.filters) {
        buildURL(
            listOfNotNull(
                includeIfNotNull(read, "read"),
                if (filters?.any() == true) "type=${filters.joinToString(",") { it.name.lowercase() }}" else null
            ),
            "/alerts", null, null, props.history
        )
    }

    override fun RBuilder.render() {
        h1 {
            +"Alerts & Notifications"
        }
        div("row") {
            div("col-lg-4") {
                attrs.jsStyle {
                    padding = "6px"
                }
                div("list-group") {
                    alertsListItem {
                        attrs.active = props.read != true
                        attrs.count = state.alertStats?.unread
                        attrs.icon = "fa-envelope"
                        attrs.text = "Unread"
                        attrs.action = {
                            toURL(false)
                        }
                    }
                    alertsListItem {
                        attrs.active = props.read == true
                        attrs.count = state.alertStats?.read
                        attrs.icon = "fa-envelope-open"
                        attrs.text = "Read"
                        attrs.action = {
                            toURL(true)
                        }
                    }
                }
                h6("mt-3") {
                    +"Filters"
                }
                div("list-group") {
                    state.alertStats?.byType?.forEach { (type, count) ->
                        alertsListItem {
                            attrs.active = props.filters?.contains(type) == true
                            attrs.count = count
                            attrs.icon = type.icon
                            attrs.text = type.name
                            attrs.action = {
                                toURL(
                                    filters = if (props.filters?.contains(type) == true) {
                                        props.filters?.minus(type)
                                    } else {
                                        (props.filters ?: emptyList()).plus(type)
                                    }
                                )
                            }
                        }
                    }
                }
            }
            div("col-lg-8 alerts") {
                ref = resultsColumn
                key = "resultsColumn"

                child(AlertInfiniteScroll::class) {
                    attrs.resultsKey = state.resultsKey
                    attrs.rowHeight = 114.0
                    attrs.itemsPerPage = 20
                    attrs.container = resultsColumn
                    attrs.loadPage = loadPage
                    attrs.renderElement = InfiniteScrollElementRenderer {
                        alert {
                            alert = it
                            read = props.read
                            hidden = state.hiddenAlerts?.contains(it?.id)
                            markAlert = { stats ->
                                setState {
                                    alertStats = stats
                                    it?.id?.let { id ->
                                        hiddenAlerts = hiddenAlerts?.plus(id)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class AlertInfiniteScroll : InfiniteScroll<UserAlert>()