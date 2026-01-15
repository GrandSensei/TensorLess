import base64
import io
import os
import subprocess
import json
import csv
import socket
from django.http import HttpResponse

from django.shortcuts import render
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
import matplotlib
matplotlib.use('Agg')  # mac being weird
import matplotlib.pyplot as plt

from web_dashboard.models import GlobalStats, PredictionLog


def index(request):


    """Renders the drawing board homepage with initial stats."""
    total_count = PredictionLog.objects.count()


    return render(request, 'index.html', {'global_count': total_count})


# ------------------------------------

@csrf_exempt
def predict_digit(request):
    if request.method == 'POST':
        try:
            data = json.loads(request.body)
            # Expecting a LIST of pixel arrays now
            # Format: [ [0,0,0...], [0,0,255...] ]
            pixel_batches = data.get('pixels')

            # Safety check: If they sent the old format (just one list), wrap it
            if pixel_batches and isinstance(pixel_batches[0], (int, float)):
                pixel_batches = [pixel_batches]

            full_prediction_string = ""
            log_ids = []

            # We will generate a graph only for the FIRST digit (to keep UI clean)
            first_graph_url = None

            # --- LOOP THROUGH EACH DIGIT ---
            for i, pixels in enumerate(pixel_batches):

                # 1. Format data for Java
                input_string = ",".join(map(str, pixels))

                host = '127.0.0.1'
                port = 9999
                prediction = "?"
                confidences = []

                # 2. Talk to Java (Standard Socket Code)
                try:
                    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
                        s.connect((host, port))
                        s.sendall((input_string + "\n").encode('utf-8'))
                        with s.makefile('r') as f:
                            output = f.read()

                        for line in output.split('\n'):
                            line = line.strip()
                            if "CONFIDENCES:" in line:
                                raw_nums = line.split(":")[1].split(",")
                                confidences = [float(x) for x in raw_nums]
                            if "PREDICTION_RESULT:" in line:
                                prediction = line.split(":")[1].strip()

                except ConnectionRefusedError:
                    return JsonResponse({'error': 'Java Brain Offline'}, status=500)

                # 3. Aggregate Results
                if prediction.isdigit():
                    full_prediction_string += prediction

                    # Save Log
                    log_entry = PredictionLog.objects.create(
                        pixel_data=json.dumps(pixels),
                        predicted_digit=int(prediction)
                    )
                    log_ids.append(log_entry.id)

                # 4. Generate Graph (Only for the first digit found)
                if i == 0 and confidences:
                    plt.figure(figsize=(6, 3))
                    plt.bar(range(10), confidences, color='#2563eb')
                    plt.title('Confidence (First Digit)', fontsize=10)
                    plt.xlabel('Digit', fontsize=8)
                    plt.ylim(0, 1.0)
                    plt.xticks(range(10))
                    plt.grid(axis='y', linestyle='--', alpha=0.1)

                    buffer = io.BytesIO()
                    plt.savefig(buffer, format='png', bbox_inches='tight')
                    buffer.seek(0)
                    image_png = buffer.getvalue()
                    buffer.close()
                    plt.close()

                    graphic = base64.b64encode(image_png).decode('utf-8')
                    first_graph_url = f"data:image/png;base64,{graphic}"

            # --- RETURN FINAL RESPONSE ---
            return JsonResponse({
                'digit': full_prediction_string,  # e.g., "12"
                'log_id': log_ids[-1] if log_ids else None,  # Return last ID for feedback
                'global_count': PredictionLog.objects.count(),
                'graph_image': first_graph_url
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




# ... existing imports ...

def download_training_data(request):
    """
    Exports verified user drawings as a CSV file.
    Format: Label, Pixel0, Pixel1, ... Pixel783
    Values: 0-255 (Integers)
    """
    # 1. Setup the CSV Response
    response = HttpResponse(content_type='text/csv')
    response['Content-Disposition'] = 'attachment; filename="mnist_user_data.csv"'

    writer = csv.writer(response)

    # 2. Write the Header as the MNIST style has one
    header = ['label'] + [f'pixel{i}' for i in range(784)]
    writer.writerow(header)

    # 3. Fetch ONLY verified data (Where the user confirmed the digit)
    # We exclude rows where 'actual_digit' is None (unverified predictions)
    logs = PredictionLog.objects.exclude(actual_digit__isnull=True)

    for log in logs:
        # Parse the JSON string back to a Python list
        pixel_list_normalized = json.loads(log.pixel_data)

        # CONVERSION: Java divides by 255, so we must multiply by 255 first.
        # We cast to int to get clean "0, 255, 128" values
        pixel_list_255 = [int(p * 255) for p in pixel_list_normalized]

        # 4. Construct the Row: [Label, Pixel1, Pixel2, ...]
        row = [log.actual_digit] + pixel_list_255

        writer.writerow(row)

    return response