{{/*
Expand the name of the chart.
*/}}
{{- define "real-estate-backend.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "real-estate-backend.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "real-estate-backend.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "real-estate-backend.labels" -}}
helm.sh/chart: {{ include "real-estate-backend.chart" . }}
{{ include "real-estate-backend.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels for backend
*/}}
{{- define "real-estate-backend.selectorLabels" -}}
app.kubernetes.io/name: {{ include "real-estate-backend.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/component: backend
{{- end }}

{{/*
Selector labels for PostgreSQL
*/}}
{{- define "real-estate-backend.postgresql.selectorLabels" -}}
app.kubernetes.io/name: {{ include "real-estate-backend.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/component: database
{{- end }}

{{/*
PostgreSQL service name
*/}}
{{- define "real-estate-backend.postgresql.serviceName" -}}
{{- printf "%s-postgres" (include "real-estate-backend.fullname" .) }}
{{- end }}

{{/*
Backend service name
*/}}
{{- define "real-estate-backend.backend.serviceName" -}}
{{- printf "%s-backend" (include "real-estate-backend.fullname" .) }}
{{- end }}
