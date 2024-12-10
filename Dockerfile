FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive
ENV ANDROID_HOME=/opt/android-sdk
ENV BUNDLE_PATH=vendor/bundle
ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/34.0.0:$BUNDLE_PATH/bin
ENV LC_ALL=en_US.UTF-8
ENV LANG=en_US.UTF-8

RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    wget \
    unzip \
    curl \
    git \
    ruby \
    ruby-dev \
    build-essential \
    && apt-get clean

RUN mkdir -p $ANDROID_HOME/cmdline-tools/latest && \
    wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip -O /tmp/cmdline-tools.zip && \
    unzip /tmp/cmdline-tools.zip -d $ANDROID_HOME/cmdline-tools/latest && \
    mv $ANDROID_HOME/cmdline-tools/latest/cmdline-tools/* $ANDROID_HOME/cmdline-tools/latest/ && \
    rm -rf $ANDROID_HOME/cmdline-tools/latest/cmdline-tools && \
    rm -f /tmp/cmdline-tools.zip && \
    yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses && \
    $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platform-tools" "platforms;android-31" "platforms;android-34" "build-tools;34.0.0"

# aapt2 requires libs that are not provided in arm64 ubuntu image
RUN  mkdir                                          /lib/x86_64-linux-gnu
COPY ci_resources/x86_64_libs/libc.so.6             /lib/x86_64-linux-gnu/libc.so.6
COPY ci_resources/x86_64_libs/libdl.so.2            /lib/x86_64-linux-gnu/libdl.so.2
COPY ci_resources/x86_64_libs/libgcc_s.so.1         /lib/x86_64-linux-gnu/libgcc_s.so.1
COPY ci_resources/x86_64_libs/libm.so.6             /lib/x86_64-linux-gnu/libm.so.6
COPY ci_resources/x86_64_libs/libpthread.so.0       /lib/x86_64-linux-gnu/libpthread.so.0
COPY ci_resources/x86_64_libs/librt.so.1            /lib/x86_64-linux-gnu/librt.so.1
RUN  mkdir                                          /lib64
COPY ci_resources/x86_64_libs/ld-linux-x86-64.so.2  /lib64/ld-linux-x86-64.so.2

RUN gem install bundler:2.5.23
RUN gem install fastlane -v 2.211.0 -N -V
RUN gem install fastlane-plugin-firebase_app_distribution -v 0.9.1 -N -V

COPY Gemfile Gemfile.lock ./
RUN bundle install --jobs=4 --retry=3 --verbose

RUN bundle exec fastlane -v

CMD ["bash"]
