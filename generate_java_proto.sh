#!/bin/bash

echo "🔧 Generando archivos protobuf para Java (route-processing-service)..."

cd /home/santiagovera/FrontEnd/e-Xiua/route-processing-service

# Compilar con Maven para generar protobuf
mvn clean compile

if [ $? -eq 0 ]; then
    echo "✅ Archivos protobuf generados exitosamente"
    echo "📁 Archivos generados en target/generated-sources/protobuf/"
    ls -la target/generated-sources/protobuf/java/route/optimization/ 2>/dev/null || echo "Verificar estructura de directorios"
else
    echo "❌ Error generando archivos protobuf"
    exit 1
fi

echo ""
echo "🚀 Para compilar el servicio Java completo:"
echo "   mvn clean package"
echo ""
echo "🐍 Para iniciar el servidor Python gRPC:"
echo "   cd ../ModeloMrlAmisPythonService"
echo "   ./start_grpc_server.sh"