/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2.demo;

import android.app.Application;
import android.net.Uri;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.UdpDataSource;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Util;


/**
 * Placeholder application to facilitate overriding Application methods for debugging and testing.
 */
public class DemoApplication extends Application {

  protected String userAgent;

  @Override
  public void onCreate() {
    super.onCreate();
    userAgent = Util.getUserAgent(this, "ExoPlayerDemo");
  }

  public DataSource.Factory buildDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
    return buildDataSourceFactory(Uri.parse("http://127.0.0.1"), bandwidthMeter );
  }

  public DataSource.Factory buildDataSourceFactory(Uri uri, DefaultBandwidthMeter bandwidthMeter) {
    return this.buildDataSourceFactory(uri, bandwidthMeter, null);
  }

  public DataSource.Factory buildDataSourceFactory(Uri uri, DefaultBandwidthMeter bandwidthMeter, UdpDataSource.EventListener eventListener) {
    DataSource.Factory dataSourceFactory;
    switch(uri.getScheme()) {
      case "udp":
        dataSourceFactory = new DefaultDataSourceFactory(this, bandwidthMeter,
                buildUdpDataSourceFactory(bandwidthMeter, eventListener));
        break;
      case "http":
      default:
        dataSourceFactory = new DefaultDataSourceFactory(this, bandwidthMeter,
                buildHttpDataSourceFactory(bandwidthMeter));
        break;
    }
    return dataSourceFactory;
  }

  public HttpDataSource.Factory buildHttpDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
    return new DefaultHttpDataSourceFactory(userAgent, bandwidthMeter);
  }

  public UdpDataSource.Factory buildUdpDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
    return this.buildUdpDataSourceFactory(bandwidthMeter, null);
  }

  public UdpDataSource.Factory buildUdpDataSourceFactory(DefaultBandwidthMeter bandwidthMeter, UdpDataSource.EventListener eventListener) {
    return new DefaultDataSourceFactory(this, bandwidthMeter, new UdpDataSourceFactory(bandwidthMeter, eventListener));
  }

  public boolean useExtensionRenderers() {
    return BuildConfig.FLAVOR.equals("withExtensions");
  }

  /**
   * Class UdpDataSourceFactory for UDP data source.
   */
  public class UdpDataSourceFactory implements DataSource.Factory {

    private DefaultBandwidthMeter bandwidthMeter;
    private UdpDataSource.EventListener eventListener;

    public UdpDataSourceFactory(DefaultBandwidthMeter bandwidthMeter, UdpDataSource.EventListener eventListener) {
      this.bandwidthMeter = bandwidthMeter;
      this.eventListener = eventListener;
    }

    @Override
    public DataSource createDataSource() {
      return new UdpDataSource(bandwidthMeter, eventListener);
    }
  }

}
