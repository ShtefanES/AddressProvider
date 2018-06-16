# AddressProvider
[![Демо](http://img.youtube.com/vi/SRMCxc1pzOA/0.jpg)](http://www.youtube.com/watch?v=SRMCxc1pzOA)

# Особенности
- Определение координат устройства
- Прямой и обратный геокодинг
- Подсказки адреса
- Установка границ для работы приложения в определенном городе
- Использование Yandex MapKit для работы с геоданными.

# Подготовка
1. Скачать проект.
2. Получить ключ для работы с MapKit https://tech.yandex.ru/mapkit/doc/3.x/concepts/android/quickstart-docpage/ .
   Для бесплатного тарифа, после генерации ключа, нужно ждать около недели пока ключ не будет активирован. Письмо об активации должно прийти на почту.
3. В MainActivity указать свой ключ для MAPKIT_API_KEY.
4. В MapFragment для START_LOCATION указать координаты центра города, для отображения экрана при старте в этой области.
5. В RegionHalper указать координаты точек, которые являются вершинами многоугольника, в котором находится город.
6. В строковых ресурсах изменить значения region_and_town, region для города и региона(используются для обработки подсказок при вводе адреса).

# Ps 
При добавлении зависимости implementation 'com.yandex.android:mapkit:3.0.0' во время сборки, появлялось сообщение об ошибки: "Caused by: com.android.builder.dexing.DexArchiveBuilderException: Error while dexing".
Решением является добавление в gradle.properties android.enableD8=false. Но во время компиляции будет предупреждение "WARNING: The option 'android.enableD8' is deprecated and should not be used anymore."
Надеюсь, что в дальнейшем программисты из Yandex это исправят и в использовании данного костыля не будет необходимости.
