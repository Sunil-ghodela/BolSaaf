from pathlib import Path

base = Path('/var/www/simplelms/backend/apps/voice')

models_block = """

class VoiceFeedback(models.Model):
    MODE_CHOICES = [
        ('clean', 'Clean'),
        ('extract_voice', 'Extract Voice'),
        ('add_background', 'Add Background'),
        ('reel', 'Reel'),
        ('video_process', 'Video Process'),
        ('unknown', 'Unknown'),
    ]

    INPUT_CHOICES = [
        ('quiet', 'Quiet'),
        ('normal', 'Normal'),
        ('noisy', 'Noisy'),
        ('unknown', 'Unknown'),
    ]

    RESULT_CHOICES = [
        ('natural', 'Natural'),
        ('quiet', 'Quiet'),
        ('artifacts', 'Artifacts'),
        ('good', 'Good'),
    ]

    app_version = models.CharField(max_length=64, blank=True, default='')
    device_model = models.CharField(max_length=120, blank=True, default='')
    os_version = models.CharField(max_length=64, blank=True, default='')
    sample_timestamp = models.CharField(max_length=64, blank=True, default='')
    mode_used = models.CharField(max_length=32, choices=MODE_CHOICES, default='unknown')
    input_type = models.CharField(max_length=16, choices=INPUT_CHOICES, default='unknown')
    result_label = models.CharField(max_length=16, choices=RESULT_CHOICES, default='good')
    issue_timestamp = models.CharField(max_length=32, blank=True, default='')
    notes = models.TextField(blank=True, default='')
    cleaned_file = models.CharField(max_length=255, blank=True, default='')
    extra_meta = models.JSONField(default=dict, blank=True)

    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['-created_at']
        verbose_name = 'Voice Feedback'
        verbose_name_plural = 'Voice Feedback'

    def __str__(self):
        return f"Feedback {self.id} ({self.result_label})"
"""

serializer_block = """

class VoiceFeedbackSerializer(serializers.ModelSerializer):
    class Meta:
        model = VoiceFeedback
        fields = [
            'id', 'app_version', 'device_model', 'os_version', 'sample_timestamp',
            'mode_used', 'input_type', 'result_label', 'issue_timestamp', 'notes',
            'cleaned_file', 'extra_meta', 'created_at'
        ]
        read_only_fields = ['id', 'created_at']

    def validate_mode_used(self, value):
        v = (value or '').strip().lower()
        allowed = {'clean','extract_voice','add_background','reel','video_process'}
        return v if v in allowed else 'unknown'

    def validate_input_type(self, value):
        v = (value or '').strip().lower()
        return v if v in {'quiet','normal','noisy'} else 'unknown'

    def validate_result_label(self, value):
        v = (value or '').strip().lower()
        allowed = {'natural','quiet','artifacts','good'}
        return v if v in allowed else 'good'
"""

view_block = """

class VoiceFeedbackView(APIView):
    permission_classes = [AllowAny]
    parser_classes = [JSONParser, FormParser, MultiPartParser]

    def post(self, request):
        serializer = VoiceFeedbackSerializer(data=request.data)
        if not serializer.is_valid():
            return Response({'status': 'error', 'errors': serializer.errors}, status=status.HTTP_400_BAD_REQUEST)

        obj = serializer.save()
        return Response({
            'status': 'ok',
            'id': obj.id,
            'created_at': obj.created_at.isoformat(),
        }, status=status.HTTP_201_CREATED)

    def get(self, request):
        limit_raw = request.query_params.get('limit', '50')
        try:
            limit = max(1, min(200, int(limit_raw)))
        except Exception:
            limit = 50
        items = VoiceFeedback.objects.all()[:limit]
        data = VoiceFeedbackSerializer(items, many=True).data
        return Response({'status': 'ok', 'count': len(data), 'items': data}, status=status.HTTP_200_OK)
"""

mp = base / 'models.py'
mt = mp.read_text()
if 'class VoiceFeedback(models.Model):' not in mt:
    mt += models_block
    mp.write_text(mt)

sp = base / 'serializers.py'
st = sp.read_text()
if 'from .models import AudioFile' in st and 'VoiceFeedback' not in st:
    st = st.replace('from .models import AudioFile', 'from .models import AudioFile, VoiceFeedback')
if 'class VoiceFeedbackSerializer(serializers.ModelSerializer):' not in st:
    st += serializer_block
sp.write_text(st)

vp = base / 'views.py'
vt = vp.read_text()
if 'from rest_framework.parsers import MultiPartParser, FormParser' in vt and 'JSONParser' not in vt:
    vt = vt.replace('from rest_framework.parsers import MultiPartParser, FormParser', 'from rest_framework.parsers import MultiPartParser, FormParser, JSONParser')
if 'HealthCheckSerializer' in vt and 'VoiceFeedbackSerializer' not in vt:
    vt = vt.replace('    HealthCheckSerializer,\n)', '    HealthCheckSerializer,\n    VoiceFeedbackSerializer,\n)')
if 'from .models import AudioFile' in vt and 'VoiceFeedback' not in vt:
    vt = vt.replace('from .models import AudioFile', 'from .models import AudioFile, VoiceFeedback')
if 'class VoiceFeedbackView(APIView):' not in vt:
    vt += view_block
vp.write_text(vt)

up = base / 'urls.py'
ut = up.read_text()
if 'VoiceFeedbackView' not in ut:
    ut = ut.replace('BackgroundCatalogView)', 'BackgroundCatalogView, VoiceFeedbackView)')
if "path('feedback/'" not in ut:
    ut = ut.replace("    path('backgrounds/', BackgroundCatalogView.as_view(), name='background_catalog'),\n", "    path('backgrounds/', BackgroundCatalogView.as_view(), name='background_catalog'),\n    path('feedback/', VoiceFeedbackView.as_view(), name='voice_feedback'),\n")
up.write_text(ut)

ap = base / 'admin.py'
at = ap.read_text()
if 'VoiceFeedback' not in at:
    at = """from django.contrib import admin
from .models import AudioFile, VoiceFeedback

@admin.register(AudioFile)
class AudioFileAdmin(admin.ModelAdmin):
    list_display = ('id', 'status', 'processing_mode', 'created_at')
    list_filter = ('status', 'processing_mode')
    search_fields = ('id',)

@admin.register(VoiceFeedback)
class VoiceFeedbackAdmin(admin.ModelAdmin):
    list_display = ('id', 'mode_used', 'input_type', 'result_label', 'device_model', 'created_at')
    list_filter = ('mode_used', 'input_type', 'result_label', 'created_at')
    search_fields = ('device_model', 'sample_timestamp', 'notes', 'cleaned_file')
"""
    ap.write_text(at)

print('patched voice backend feedback files')
