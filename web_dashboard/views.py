import base64
import io
import os
import subprocess
import json
import csv
import socket
from django.http import HttpResponse

from django.shortcuts import render  # <--- This was likely missing or unused
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
import matplotlib
matplotlib.use('Agg')  # <--- THIS LINE IS THE FIX
import matplotlib.pyplot as plt

from web_dashboard.models import GlobalStats, PredictionLog


def index(request):


    """Renders the drawing board homepage with initial stats."""
    total_count = PredictionLog.objects.count()

    # 2. Pass it to the template
    return render(request, 'index.html', {'global_count': total_count})


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

            input_string = ",".join(map(str, pixel_data))

            # --- NEW SOCKET LOGIC START ---
            host = '127.0.0.1'
            port = 9999

            prediction = "?"
            confidences = []
            output = ""

            try:
                # 1. Connect to the Java Server
                with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
                    s.connect((host, port))

                    # 2. Send the pixels (with a newline at the end)
                    s.sendall((input_string + "\n").encode('utf-8'))

                    # 3. Receive the response (Read until server closes or done)
                    with s.makefile('r') as f:
                        output = f.read()


                    # 4. Parse the response (Same logic as before)
                    for line in output.split('\n'):
                        line = line.strip()
                        if "CONFIDENCES:" in line:
                            raw_nums = line.split(":")[1].split(",")
                            confidences = [float(x) for x in raw_nums]
                        if "PREDICTION_RESULT:" in line:
                            prediction = line.split(":")[1].strip()

            except ConnectionRefusedError:
                return JsonResponse({'error': 'Java Server is offline. Please run "java Server" in a terminal.'},
                                    status=500)


            # --- GRAPH GENERATION ---
            graph_url = None
            if confidences:
                # 1. Setup the plot (Clean style)
                plt.figure(figsize=(6, 3))  # Width, Height
                plt.bar(range(10), confidences, color='#2563eb')  # Blue bars

                # 2. Styling
                plt.title('Neural Network Confidence', fontsize=10)
                plt.xlabel('Digit (0-9)', fontsize=8)
                plt.ylabel('Probability', fontsize=8)
                plt.xticks(range(10))  # Show numbers 0-9 on x-axis
                plt.ylim(0, 1.0)  # Scale from 0% to 100%
                plt.grid(axis='y', linestyle='--', alpha=0.1)

                # 3. Save to a memory buffer (not a file)
                buffer = io.BytesIO()
                plt.savefig(buffer, format='png', bbox_inches='tight')
                buffer.seek(0)

                # 4. Convert to Base64 String
                image_png = buffer.getvalue()
                buffer.close()
                plt.close()  # Free memory

                graphic = base64.b64encode(image_png).decode('utf-8')
                graph_url = f"data:image/png;base64,{graphic}"

            '''
            # --- DEBUGGING: PRINT EXACTLY WHAT JAVA SAID ---
            print("-" * 20)
            print("JAVA STDOUT:", process.stdout)
            print("JAVA STDERR:", process.stderr)
            print("-" * 20)
            '''



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

            return JsonResponse({
                'digit': prediction,
                'log_id': log_id,
                'global_count': total_count,
                'graph_image': graph_url,
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

    # 2. Write the Header
    # (Your Java ExcelParse skips the first line, so we need a header row)
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