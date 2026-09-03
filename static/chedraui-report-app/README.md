# Chedraui Report App (Standalone Swing)

Aplicacion Java Swing standalone (sin Maven, sin Nexus) que consume el endpoint
de Chedraui y genera un reporte HTML con productos.

## Endpoint consumido

`GET https://www.chedraui.com.mx/api/catalog_system/pub/products/search?fq=C:/10/1005`

Headers usados:
- `Accept: application/json`
- `Authorization: application/json` (como fue solicitado)

## Campos del reporte

- `productId`
- `productName`
- `brand`
- `productTitle`
- `releaseDate`
- `categories`
- `categoriesIds`
- `link`
- `Maximo de venta`
- `Price`
- `ListPrice`
- `PriceWithoutDiscount`
- `AvailableQuantity`
- `IsAvailable`

## Compilar y ejecutar (sin Maven)

Desde esta carpeta:

```bash
javac ChedrauiSwingApp.java
java ChedrauiSwingApp
```

## Uso

1. Clic en `Consumir endpoint` para cargar productos.
2. Clic en `Generar HTML` para guardar el reporte.
