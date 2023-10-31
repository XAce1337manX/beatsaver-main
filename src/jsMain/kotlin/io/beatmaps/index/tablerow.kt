package io.beatmaps.index

import external.routeLink
import io.beatmaps.api.MapDetail
import io.beatmaps.api.MapVersion
import io.beatmaps.shared.diffIcons
import io.beatmaps.shared.links
import io.beatmaps.shared.uploaderWithInfo
import react.Props
import react.RefObject
import react.dom.img
import react.dom.p
import react.dom.small
import react.dom.td
import react.dom.tr
import react.fc

external interface TableRowProps : Props {
    var map: MapDetail
    var version: MapVersion?
    var modal: RefObject<ModalComponent>
}

val beatmapTableRow = fc<TableRowProps> { props ->
    tr {
        td {
            img(src = props.version?.coverURL, alt = "Cover Image", classes = "cover") {
                attrs.width = "100"
                attrs.height = "100"
            }
        }
        td {
            routeLink("/maps/${props.map.id}") {
                +props.map.name
            }
            p {
                uploaderWithInfo {
                    attrs.map = props.map
                    attrs.version = props.version
                }
                small {
                    +props.map.description.replace("\n", " ")
                }
            }
        }
        td("diffs") {
            diffIcons.invoke {
                attrs.diffs = props.version?.diffs
            }
        }
        td("links") {
            links {
                attrs.map = props.map
                attrs.version = props.version
                attrs.modal = props.modal
            }
        }
    }
}
