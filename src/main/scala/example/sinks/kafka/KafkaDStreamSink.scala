// Copyright (C) 2011-2012 the original author or authors.
// See the LICENCE.txt file distributed with this work for additional
// information regarding copyright ownership.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package example.sinks.kafka

import example.sinks.DStreamSink
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.DStream

class KafkaDStreamSink[A](config: KafkaDStreamSinkConfig) extends DStreamSink[A] {

  private val KeySerializer = "org.apache.kafka.common.serialization.StringSerializer"
  private val ValueSerializer = "org.apache.kafka.common.serialization.StringSerializer"

  private val producer = new KafkaProducerSingleton(
    Map(
      "bootstrap.servers" -> config.bootstrapServers,
      "acks" -> config.acks,
      "key.serializer" -> KeySerializer,
      "value.serializer" -> ValueSerializer
    )
  )

  override def write(ssc: StreamingContext, topic: String, stream: DStream[A]): Unit = {
    val topicVar = ssc.sparkContext.broadcast(topic)
    val producerVar = ssc.sparkContext.broadcast(producer)

    stream.foreachRDD { rdd =>
      rdd.foreach { record =>
        val topic = topicVar.value
        val producer = producerVar.value.producerHolder

        // TODO: callback handling
        producer.send(new ProducerRecord[String, String](topic, record.toString))

        // TODO: handle record metadata
        ()
      }
    }
  }

}

object KafkaDStreamSink {
  def apply[A](config: KafkaDStreamSinkConfig): KafkaDStreamSink[A] = new KafkaDStreamSink[A](config)
}
