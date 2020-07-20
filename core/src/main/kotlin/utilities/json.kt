package org.jetbrains.dokka.utilities

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import org.jetbrains.dokka.DokkaConfiguration.SourceRoot
import org.jetbrains.dokka.SourceRootImpl
import java.io.File
import com.fasterxml.jackson.core.type.TypeReference as JacksonTypeReference

private val objectMapper = run {
    val module = SimpleModule().apply {
        addSerializer(FileSerializer)
        addSerializer(SourceRoot::class.java, SourceRootSerializer)
        addDeserializer(SourceRootImpl::class.java, SourceRootImplDeserializer)
        addDeserializer(SourceRoot::class.java, SourceRootImplDeserializer)
    }
    jacksonObjectMapper()
        .registerModule(module)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}

@PublishedApi
internal class TypeReference<T> private constructor(
    internal val jackson: JacksonTypeReference<T>
) {
    companion object {
        internal inline operator fun <reified T> invoke(): TypeReference<T> = TypeReference(jacksonTypeRef())
    }
}

@PublishedApi
internal fun toJsonString(value: Any): String = objectMapper.writeValueAsString(value)

@PublishedApi
internal inline fun <reified T : Any> parseJson(json: String): T = parseJson(json, TypeReference())


@PublishedApi
internal fun <T : Any> parseJson(json: String, typeReference: TypeReference<T>): T =
    objectMapper.readValue(json, typeReference.jackson)


private object FileSerializer : StdScalarSerializer<File>(File::class.java) {
    override fun serialize(value: File, g: JsonGenerator, provider: SerializerProvider) {
        g.writeString(value.path)
    }
}

private object SourceRootSerializer : StdScalarSerializer<SourceRoot>(SourceRoot::class.java) {
    override fun serialize(value: SourceRoot, g: JsonGenerator, provider: SerializerProvider) {
        g.writeString(value.path)
    }
}

private object SourceRootImplDeserializer : StdScalarDeserializer<SourceRootImpl>(SourceRootImpl::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): SourceRootImpl = SourceRootImpl(p.text)
}
