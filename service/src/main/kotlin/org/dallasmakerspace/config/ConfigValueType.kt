package org.dallasmakerspace.config

import kotlinx.serialization.KSerializer

sealed class ConfigValueType {
  object StringType : ConfigValueType()

  object IntType : ConfigValueType()

  object DateType : ConfigValueType()

  object BooleanType : ConfigValueType()

  data class ComplexType<T>(val serializer: KSerializer<T>) : ConfigValueType()
}
