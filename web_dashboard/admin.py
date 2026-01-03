from django.contrib import admin

# Register your models here.
from .models import GlobalStats, PredictionLog

@admin.register(GlobalStats)
class GlobalStatsAdmin(admin.ModelAdmin):
    list_display = ('total_predictions', 'last_updated')

@admin.register(PredictionLog)
class PredictionLogAdmin(admin.ModelAdmin):
    list_display = ('predicted_digit', 'timestamp')
    list_filter = ('predicted_digit',) # Adds a cool filter sidebar!