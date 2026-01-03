import os
import subprocess
import json
from django.shortcuts import render  # <--- This was likely missing or unused
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt

from web_dashboard.models import GlobalStats, PredictionLog


def index(request):
    """Renders the drawing board homepage."""
    return render(request, 'index.html')


# ------------------------------------

@csrf_exempt
def predict_digit(request):
    if request.method == 'POST':
        try:
            data = json.loads(request.body)
            pixel_data = data.get('pixels')
            input_string = ",".join(map(str, pixel_data))

            # --- PATH SETUP ---
            base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
            java_bin_path = os.path.join(base_dir, 'java_core', 'bin')

            # --- RUN JAVA ---
            process = subprocess.run(
                ['java', '-cp', '.', 'Predictor', input_string],
                cwd=java_bin_path,
                capture_output=True,
                text=True
            )

            # --- DEBUGGING: PRINT EXACTLY WHAT JAVA SAID ---
            print("-" * 20)
            print("JAVA STDOUT:", process.stdout)
            print("JAVA STDERR:", process.stderr)
            print("-" * 20)

            # --- PARSE OUTPUT ---
            output = process.stdout
            prediction = "?"

            for line in output.split('\n'):
                if "PREDICTION_RESULT:" in line:
                    prediction = line.split(":")[1].strip()

            # If prediction is still ?, send the raw error back to UI to see
            if prediction == "?":
                prediction = "Err"

            total_count = PredictionLog.objects.count()


            # 2. Save the Individual Log (NEW CODE)
            # 'prediction' is the string (e.g. "7") we got from Java
            log_id = None
            if prediction.isdigit():
                # Save the pixels AND the prediction
                log_entry = PredictionLog.objects.create(
                    pixel_data=json.dumps(pixel_data),  # Save input as string
                    predicted_digit=int(prediction)
                )
                log_id = log_entry.id
            # --- NEW LOGIC END ---

            return JsonResponse({
                'digit': prediction,
                'log_id': log_id,# Send ID so frontend can reference it later
                'global_count': total_count,
                'raw_output': output
            })
        except Exception as e:
            print("PYTHON ERROR:", e)
            return JsonResponse({'error': str(e)}, status=500)

    return JsonResponse({'error': 'Invalid request'}, status=400)


@csrf_exempt
def submit_feedback(request):
    if request.method == 'POST':
        data = json.loads(request.body)
        log_id = data.get('log_id')
        is_correct = data.get('is_correct')
        correct_digit = data.get('correct_digit')  # If they corrected it

        try:
            entry = PredictionLog.objects.get(id=log_id)
            entry.is_correct = is_correct

            if is_correct:
                entry.actual_digit = entry.predicted_digit
            else:
                entry.actual_digit = correct_digit

            entry.save()
            return JsonResponse({'status': 'success'})
        except PredictionLog.DoesNotExist:
            return JsonResponse({'status': 'error', 'message': 'Log not found'})
