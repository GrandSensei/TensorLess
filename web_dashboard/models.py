from django.db import models

# Create your models here.
from django.db import models

class GlobalStats(models.Model):
    total_predictions = models.IntegerField(default=0)
    last_updated = models.DateTimeField(auto_now=True)

    # Helper to always get the single row we need
    @classmethod
    def get_stats(cls):
        obj, created = cls.objects.get_or_create(id=1)
        return obj


class PredictionLog(models.Model):
    # The Input (The expensive part!)
    pixel_data = models.TextField()  # Stores the "[0.0, 0.5, ...]" JSON string

    # The Model's Guess
    predicted_digit = models.IntegerField()

    # The Human's Correction (Null means "User hasn't checked yet")
    actual_digit = models.IntegerField(null=True, blank=True)

    # Did the user confirm it?
    is_correct = models.BooleanField(default=False)

    timestamp = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"Pred: {self.predicted_digit} | Actual: {self.actual_digit}"