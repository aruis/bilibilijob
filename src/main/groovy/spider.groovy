import groovy.json.JsonOutput
import groovy.json.JsonParser
import groovy.json.JsonSlurper
import groovy.sql.Sql
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import org.postgresql.util.PGobject

import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

def url = "https://jobs.bilibili.com/api/auth/v1/csrf/token"


def time = "2020-12-09T07:33:33Z"
//def date = Date.parse("yyyy-MM-dd'T'HH:mm:ssZ", time)
//println(Timestamp.valueOf(date))

println LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"))

def vertx = Vertx.vertx()
def options = new WebClientOptions()
        .setDefaultHost("jobs.bilibili.com")
        .setSsl(true)
        .setTrustAll(true)
        .setDefaultPort(443)

def client = WebClient.create(vertx, options)
def db = Sql.newInstance("jdbc:postgresql://127.0.0.1:54321/bilibilijob", "liurui", "liurui", "org.postgresql.Driver")


client.get("/api/auth/v1/csrf/token")
        .putHeader("x-appkey", "ops.ehr-api.auth")
        .putHeader("x-usertype", "2")
        .send({ ar ->
            def resp = ar.result()
            println(resp.body().toString())
        })

2000.times {
    client.get("/api/srs/position/detail/$it")
            .putHeader("x-appkey", "ops.ehr-api.auth")
            .putHeader("x-usertype", "2")
            .putHeader("x-csrf", '$2b$10$CrqxsBuj33b8Cu9PdGeJ0.ms3VxOT.quT2EZW1hPkfIRFxyAlD0D6')
            .send({ ar ->
                def resp = ar.result()
                def body = resp.body().toString()
                def json = new JsonSlurper().parseText(body)
                Map data = json.data

                println(data)

                if (data.id && data.id.toInteger() > 0) {

                    def pushTime = LocalDateTime.parse(data.pushTime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"))

                    PGobject jsonObject = new PGobject();
                    jsonObject.setType("json");
                    jsonObject.setValue(JsonOutput.toJson(data))

                    db.executeInsert("""
insert into jobs (id, position_name, position_description, position_type_name, post_code_name, push_time, work_location, json_data) 
values (?,?,?,?,?,?,?,?)
on conflict (id) do update set  (id, position_name, position_description, position_type_name, post_code_name, push_time, work_location, json_data,t_update)
= (?,?,?,?,?,?,?,?,now())
""", [
                            data.id.toInteger(), data.positionName, data.positionDescription, data.positionTypeName, data.postCodeName, pushTime, data.workLocation, jsonObject,
                            data.id.toInteger(), data.positionName, data.positionDescription, data.positionTypeName, data.postCodeName, pushTime, data.workLocation, jsonObject,
                    ])
                }
            })
}





