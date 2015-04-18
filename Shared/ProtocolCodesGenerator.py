#!/usr/bin/env python3
import configparser

JAVA_TEMPLATE = '''
package znet;

public class ProtocolCodes {{
{}
}}
'''

PYTHON_TEMPLATE = '''
import enum
{}
'''

def generate_java_enum(enum_config, enum_name):
    enum_members_list = []
    for enum_member in enum_config[enum_name]:
        enum_member_value = enum_config[enum_name][enum_member]
        enum_members_list.append('\t\t{}'.format(
            enum_member,  enum_member_value))
    enums_code = '\tpublic enum {} {{\n{};\n\t}}\n'.format(
            enum_name, ',\n'.join(enum_members_list))
    return enums_code

def generate_python_enum(enum_config, enum_name):
    enum_members_list = []
    for enum_member in enum_config[enum_name]:
        enum_member_value = enum_config[enum_name][enum_member]
        enum_members_list.append('    {} = {}\n'.format(
            enum_member, enum_member_value))
    enums_code = 'class {}(enum.Enum):\n{}\n'.format(
        enum_name, ''.join(enum_members_list))
    return enums_code


def main():
    input_file = 'ProtocolCodes.ini'
    java_output_file = '../Client/ProtocolCodes.java'
    python_output_file = '../Server/ProtocolCodes.py'
    java_enums_code = ''
    python_enums_code = ''

    enum_config = configparser.ConfigParser()
    enum_config.read(input_file)
    for enum in enum_config.sections():
        java_enums_code += generate_java_enum(enum_config, enum)
        python_enums_code += generate_python_enum(enum_config, enum)

    with open(java_output_file, 'wb') as f:
        f.write(JAVA_TEMPLATE.format(java_enums_code).encode('utf-8'))
        print("Genereated {}".format(java_output_file))

    with open(python_output_file, 'wb') as f:
        f.write(PYTHON_TEMPLATE.format(python_enums_code).encode('utf-8'))
        print("Genereated {}".format(python_output_file))

if __name__ == '__main__':
    main()
    
