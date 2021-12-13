package hello
import com.google.api.core.ApiFuture;
import com.google.cloud.ServiceOptions;
import com.google.cloud.bigquery.storage.v1.*;
import com.google.protobuf.Descriptors;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.Instant;

class WriteCommittedStream(projectId: String?, datasetName: String?, tableName: String?) {
    var jsonStreamWriter: JsonStreamWriter? = null
    fun send(arena: Arena): ApiFuture<AppendRowsResponse> {
        val now: Instant = Instant.now()
        val jsonArray = JSONArray()
        arena.state.forEach { url, playerState ->
            val jsonObject = JSONObject()
            jsonObject.put("x", playerState.x)
            jsonObject.put("y", playerState.y)
            jsonObject.put("direction", playerState.direction)
            jsonObject.put("wasHit", playerState.wasHit)
            jsonObject.put("score", playerState.score)
            jsonObject.put("player", url)
            jsonObject.put("timestamp", now.getEpochSecond() * 1000 * 1000)
            jsonArray.put(jsonObject)
        }
        val jw = jsonStreamWriter!!
        return jw.append(jsonArray)
    }

    init {
        BigQueryWriteClient.create().use { client ->
            val stream: WriteStream = WriteStream.newBuilder().setType(WriteStream.Type.COMMITTED).build()
            val parentTable: TableName = TableName.of(projectId, datasetName, tableName)
            val createWriteStreamRequest: CreateWriteStreamRequest = CreateWriteStreamRequest.newBuilder()
                .setParent(parentTable.toString())
                .setWriteStream(stream)
                .build()
            val writeStream: WriteStream = client.createWriteStream(createWriteStreamRequest)
            jsonStreamWriter = JsonStreamWriter.newBuilder(writeStream.getName(), writeStream.getTableSchema()).build()
        }
    }
}