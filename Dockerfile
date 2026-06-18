# Dockerfile для автоматической сборки модифицированного «Росбраузера» на базе Chromium Stable для Android
# Основан на архитектурных требованиях: импортозамещение, суверенная безопасность и автономность от Google Play Services.

FROM ubuntu:20.04

# Полностью исключаем интерактивные диалоги при установке пакетов
ENV DEBIAN_FRONTEND=noninteractive
ENV FORCE_UNSAFE_CONFIGURE=1

# Обновляем менеджер пакетов и устанавливаем критические утилиты сборщика
RUN apt-get update && apt-get install -y \
    binutils \
    git \
    python3 \
    python3-pip \
    python-is-python3 \
    wget \
    curl \
    lsb-release \
    sudo \
    tzdata \
    locales \
    libxml2 \
    gperf \
    bison \
    flex \
    build-essential \
    openjdk-11-jdk \
    && rm -rf /var/lib/apt/lists/*

# Генерация локалей (для совместимости кодировок при работе скриптов)
RUN locale-gen ru_RU.UTF-8 && locale-gen en_US.UTF-8
ENV LANG=en_US.UTF-8
ENV LC_ALL=en_US.UTF-8

# Создаем рабочего пользователя "developer", так как gclient/depot_tools запрещено запускать под root
RUN useradd -m -s /bin/bash developer && \
    echo "developer ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers

USER developer
WORKDIR /home/developer

# 1. Установка утилит Chromium - depot_tools
RUN git clone --depth=1 https://chromium.googlesource.com/chromium/tools/depot_tools.git /home/developer/depot_tools
ENV PATH="/home/developer/depot_tools:${PATH}"
ENV DEPOT_TOOLS_UPDATE=0

# Создаем рабочий каталог для исходников Chromium
RUN mkdir -p /home/developer/chromium
WORKDIR /home/developer/chromium

# 2. Инициализация и получение исходного кода Chromium Stable для Android
# Ограничиваем историю коммитов (--no-history) для ускорения скачивания и экономии места
RUN fetch --nohooks --no-history android

WORKDIR /home/developer/chromium/src

# Переключаемся на последнюю стабильную ветку (пример стабильного релиза - Chromium 125.0.6422.112)
RUN git checkout tags/125.0.6422.112 -b stable-rosbrowser

# Рекурсивно вытягиваем сабмодули и внешние зависимости (WebRTC, NDK, SDK)
RUN gclient sync --with_branch_heads --with_tags --force --nohooks

# Установка системных зависимостей для сборки под Android (запускается скриптом Chromium)
USER root
RUN ./build/install-build-deps-android.sh
USER developer

# Запускаем окончательную синхронизацию хуков (скачивание вспомогательных компиляторов, Clang, Rust toolchain)
RUN gclient runhooks

# 3. Перенос и применение патчей отечественного импортозамещения
# Копируем патчи, подготовленные разработчиком (помещаются в Docker-контекст при сборке)
COPY --chown=developer:developer ./patches/ /home/developer/chromium/patches/

# Применяем патчи. Одно из ключевых решений:
# - Выпиливание зависимостей от Google Play Services (вход в аккаунт, SafeBrowsing)
# - Интеграция Яндекс.ID OAuth 2.0
# - Внедрение отечественного поисковика Яндекс по умолчанию
# - Поддержка российских шифровальных ГОСТ алгоритмов (libgost / TLS)
# - Устранение NullPointerException блокировок и утечек памяти на старте страницы
RUN for patch_file in /home/developer/chromium/patches/*.patch; do \
        if [ -f "$patch_file" ]; then \
            echo "Applying patch: $patch_file" && \
            git apply "$patch_file" || exit 1; \
        fi \
    done

# 4. Создание конфигурационного файла сборки GN (args.gn)
# Настраиваем оптимизированную, готовую к релизу архитектуру без Google Play Services
RUN mkdir -p out/Default
RUN echo 'target_os = "android"' > out/Default/args.gn && \
    echo 'target_cpu = "arm64"' >> out/Default/args.gn && \
    echo 'is_debug = false' >> out/Default/args.gn && \
    echo 'is_component_build = false' >> out/Default/args.gn && \
    echo 'is_official_build = true' >> out/Default/args.gn && \
    echo 'symbol_level = 0' >> out/Default/args.gn && \
    echo 'enable_vr = false' >> out/Default/args.gn && \
    echo 'proprietary_codecs = true' >> out/Default/args.gn && \
    echo 'ffmpeg_branding = "Chrome"' >> out/Default/args.gn && \
    echo 'enable_resource_allowlist_generation = false' >> out/Default/args.gn && \
    echo 'enable_nacl = false' >> out/Default/args.gn && \
    echo 'dfm_format_local_workaround = true' >> out/Default/args.gn && \
    echo 'enable_gost_tls = true' >> out/Default/args.gn && \
    echo 'exclude_unsupported_google_play_services = true' >> out/Default/args.gn

# Генерируем конфигурационные файлы сборщика на основе GN-аргументов
RUN gn gen out/Default

# 5. Сборка приложения с помощью autoninja
# Собирает официальный установочный пакет Android APK
RUN autoninja -C out/Default chrome_public_apk

# 6. Извлечение собранного готового бинарного пакета и подписание
USER root
RUN mkdir -p /output
RUN cp out/Default/apks/ChromePublic.apk /output/rosbrowser-release-unsigned.apk

# Генерируем локальный суверенный ключ подписи и подписываем APK с помощью apksigner
WORKDIR /output
RUN keytool -genkey -v -keystore rosbrowser.keystore -alias rosbrowser_key \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass "rosbrowser_pass1" -keypass "rosbrowser_pass1" \
    -dname "CN=Rosbrowser, OU=Engineering, O=Mintsifry, L=Moscow, C=RU"

RUN apksigner sign --ks rosbrowser.keystore --ks-key-alias rosbrowser_key \
    --ks-pass pass:rosbrowser_pass1 --key-pass pass:rosbrowser_pass1 \
    --out rosbrowser-release-signed.apk rosbrowser-release-unsigned.apk

VOLUME ["/output"]
CMD ["echo", "Сборка Росбраузера завершена успешно! Подписанный APK лежит в директории /output/rosbrowser-release-signed.apk"]
