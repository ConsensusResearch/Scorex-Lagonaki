package scorex.api.http

import java.net.InetSocketAddress
import java.net.HttpURLConnection._
import javax.ws.rs.Path

import akka.actor.ActorRefFactory
import akka.pattern.ask
import com.wordnik.swagger.annotations._
import play.api.libs.json.{JsString, JsArray, Json}
import scorex.app.Application
import scorex.network.Handshake
import scorex.network.peer.{PeerInfo, PeerManager}
import spray.routing.Route

import scala.concurrent.ExecutionContext.Implicits.global

@Api(value = "/peers", description = "Get info about peers", position = 2)
case class PeersHttpService(override val application: Application)(implicit val context: ActorRefFactory)
  extends ApiRoute with CommonApiFunctions {

  override lazy val route =
    pathPrefix("peers") {
      allPeers ~ connectedPeers ~ blacklistedPeers
    }

  @Path("/all")
  @ApiOperation(value = "Peer list", notes = "Peer list", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = HTTP_OK, message = "Json with peer list or error")
  ))
  def allPeers: Route = path("all") {
    jsonRoute {
      (application.peerManager ? PeerManager.GetAllPeers)
        .mapTo[Map[InetSocketAddress, PeerInfo]]
        .map { peers =>
          Json.arr(peers.map { case (address, peerInfo) =>
            Json.obj(
              "address" -> address.toString,
              "nodeName" -> (peerInfo.nodeName.getOrElse("N/A"): String),
              "nodeNonce" -> (peerInfo.nonce.map(_.toString).getOrElse("N/A"): String)
            )
          }).toString()
        }
    }
  }

  @Path("/connected")
  @ApiOperation(value = "Connected peers list", notes = "Connected peers list", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = HTTP_OK, message = "Json with connected peers or error")
  ))
  def connectedPeers: Route = path("connected") {
    jsonRoute {
      (application.peerManager ? PeerManager.GetConnectedPeers)
        .mapTo[Seq[Handshake]]
        .map { handshakes =>
          val peerData = Json.arr(handshakes.map { handshake =>
            Json.obj(
              "declaredAddress" -> handshake.declaredAddress.getOrElse("N/A").toString,
              "peerName" -> handshake.nodeName,
              "peerNonce" -> handshake.nodeNonce
            )
          })
          Json.obj("peers" -> peerData).toString()
        }
    }
  }

  @Path("/blacklisted")
  @ApiOperation(value = "Blacklisted peers list", notes = "Connected peers list", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = HTTP_OK, message = "Json with connected peers or error")
  ))
  def blacklistedPeers: Route = path("blacklisted") {
    jsonRoute {
      (application.peerManager ? PeerManager.GetBlacklistedPeers)
        .mapTo[Seq[String]]
        .map { peers =>
          JsArray(peers.map(i => JsString(i))).toString()
        }
    }
  }


}
