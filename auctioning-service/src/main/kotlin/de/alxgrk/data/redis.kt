package de.alxgrk.data

import redis.clients.jedis.search.FieldName
import redis.clients.jedis.search.Schema
import java.util.*

fun tag(name: String) = Schema.Field(FieldName.of("$.$name").`as`(name), Schema.FieldType.TAG)
fun numeric(name: String) = Schema.Field(FieldName.of("$.$name").`as`(name), Schema.FieldType.NUMERIC)
fun tagArray(name: String) = Schema.Field(FieldName.of("$.$name.*").`as`(name), Schema.FieldType.TAG)
fun text(name: String) = Schema.Field(FieldName.of("$.$name").`as`(name), Schema.FieldType.TEXT)
fun Schema.addMonetaryAmountField(name: String) = apply {
    addField(Schema.Field(FieldName.of("$.$name.amount").`as`("$name.amount"), Schema.FieldType.NUMERIC))
    addField(Schema.Field(FieldName.of("$.$name.currency").`as`("$name.currency"), Schema.FieldType.TAG))
}

fun UUID.toRedisEscapedString() = toString().replace("-", "\\-")
