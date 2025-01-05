{{- define "milvus.serverConfig" -}}
# Default values are used when you make no changes to the following parameters.
version: {{ .Values.version }} 

network:
  bind.address: 0.0.0.0                  # milvus server ip address (IPv4)
  bind.port: 19530                       # milvus server port, must in range [1025, 65534]
  http.enable: true
  http.port: 8080

db_config:
{{- if not .Values.backendURL }}
  {{- if .Values.mysql.enabled }}
  backend_url: {{ template "milvus.mysqlURL" . }}       # URI format: dialect://username:password@host:port/database
  {{- else }}
  backend_url: {{ template "milvus.sqliteURL" . }}       # URI format: dialect://username:password@host:port/database
  {{- end }}
{{- else }}
  backend_url: {{ .Values.backendURL }}       # URI format: dialect://username:password@host:port/database
{{- end }}
                                    # Keep 'dialect://:@:/', and replace other texts with real values
                                    # Replace 'dialect' with 'mysql' or 'sqlite'
  preload_table:                    # preload data at startup, '*' means load all tables, empty value means no preload
                                    # you can specify preload tables like this: table1,table2,table3
  auto_flush_interval: {{ .Values.autoFlushInterval }}

storage:
  path: {{ .Values.primaryPath }}         # path used to store data and meta
  secondary_path:                   # path used to store data only, split by semicolon

metric:
  enable: {{ .Values.metrics.enabled }}             # enable monitoring or not, must be a boolean
  address: {{ .Values.metrics.address }}
  port: {{ .Values.metrics.port }}                      # port prometheus uses to fetch metrics, must in range [1025, 65534]

cache:
  cache_size: {{ .Values.cpuCacheCapacity }}            # GB, size of CPU memory used for cache, must be a positive integer
  cache_insert_data: {{ .Values.cacheInsertData }}          # whether to load inserted data into cache, must be a boolean
  insert_buffer_size: {{ .Values.insertBufferSize }}             # GB, maximum insert buffer size allowed, must be a positive integer
                                    # sum of insert_buffer_size and cpu_cache_capacity cannot exceed total memory

engine_config:
  use_blas_threshold: {{ .Values.useBLASThreshold }}          # if nq <  use_blas_threshold, use SSE, faster with fluctuated response times
                                    # if nq >= use_blas_threshold, use OpenBlas, slower with stable response times
  gpu_search_threshold: {{ .Values.gpuSearchThreshold }}        # threshold beyond which the search computation is executed on GPUs only

gpu_resource_config:
  enable: {{ .Values.gpu.enabled }}  # whether to enable GPU resources
  cache_capacity: {{ .Values.gpu.cacheCapacity }}                 # GB, size of GPU memory per card used for cache, must be a positive integer
  {{- with .Values.gpu.searchResources }}
  search_resources:                 # define the GPU devices used for search computation, must be in format gpux
    {{- toYaml . | nindent 4 }}
  {{- end }}
  {{- with .Values.gpu.buildIndexResources }}
  build_index_resources:            # define the GPU devices used for index building, must be in format gpux
    {{- toYaml . | nindent 4 }}
  {{- end }}

wal_config:
  enable:  {{ .Values.wal.enabled }}
  recovery_error_ignore: {{ .Values.wal.ignoreErrorLog }}
  buffer_size: {{ .Values.wal.bufferSize }}
  record_size: {{ .Values.wal.recordSize }}
  wal_path: {{ .Values.wal.path }}
{{- end }}
