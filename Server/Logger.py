import sys
from datetime import datetime

class Logger(object):
    def __init__(self, log_file_path):
        self.log_file = open(log_file_path, 'a')

    def log(self, text):
        time_prompt = datetime.now().strftime('[%Y-%m-%d %H:%M:%S]')
        output_text = (u'{} {}\n'.format(time_prompt, text)).encode('utf-8')
        sys.stdout.write(output_text)
        self.log_file.write(output_text)
        self.log_file.flush()
