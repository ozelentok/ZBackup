import sys
from datetime import datetime


class Logger:
    def __init__(self, log_file_path):
        self._log_file = open(log_file_path, 'a')

    def log(self, text):
        time_prompt = datetime.now().strftime('[%Y-%m-%d %H:%M:%S]')
        output_text = '{} {}\n'.format(time_prompt, text)
        sys.stdout.write(output_text)
        self._log_file.write(output_text)
        self._log_file.flush()
