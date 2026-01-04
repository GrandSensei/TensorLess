from django.urls import path
from . import views

urlpatterns = [
    path('', views.index, name='index'),
    path('feedback/', views.submit_feedback, name='feedback'),# The homepage
    path('predict/', views.predict_digit, name='predict'), # The hidden endpoint API

    path('download-data/', views.download_training_data, name='download_data'),
]
